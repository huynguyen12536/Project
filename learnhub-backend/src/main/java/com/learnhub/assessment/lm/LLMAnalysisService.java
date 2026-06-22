package com.learnhub.assessment.lm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.github.entity.RepositorySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LLMAnalysisService {

    @Value("${learnhub.llm.api-key:}")
    private String llmApiKey;

    @Value("${learnhub.llm.endpoint:https://api.anthropic.com/v1/messages}")
    private String llmEndpoint;

    @Value("${learnhub.llm.model:claude-3-5-sonnet-20241022}")
    private String llmModel;

    @Value("${learnhub.llm.max-tokens:1000}")
    private Integer maxTokens;

    @Value("${learnhub.llm.timeout-seconds:10}")
    private Integer timeoutSeconds;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LLMAnalysisService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Analyze repository via Anthropic Claude API.
     * Returns structured competency scoring deltas and gap analysis text.
     *
     * Fallback to DEFAULT on API error (timeout, rate limit, network failure).
     * Never throws — always returns valid LLMAnalysisResult.
     */
    public LLMAnalysisResult analyzeRepository(RepositorySnapshot snapshot) {
        if (isApiDisabled()) {
            log.debug("LLM API disabled (no API key), using default result");
            return LLMAnalysisResult.DEFAULT;
        }

        try {
            String prompt = buildAnalysisPrompt(snapshot);
            String response = callClaudeAPI(prompt);
            return parseLLMResponse(response);
        } catch (RestClientException e) {
            log.warn("LLM API timeout or connection error: {}", e.getMessage());
            return LLMAnalysisResult.DEFAULT;
        } catch (Exception e) {
            log.warn("LLM analysis failed ({}), using default result: {}", e.getClass().getSimpleName(), e.getMessage());
            return LLMAnalysisResult.DEFAULT;
        }
    }

    private boolean isApiDisabled() {
        return llmApiKey == null || llmApiKey.isBlank();
    }

    private String buildAnalysisPrompt(RepositorySnapshot snapshot) {
        String topFiles = snapshot.getTopFiles().stream()
            .map(f -> "- " + f.getName() + " (" + f.getLineCount() + " lines)")
            .collect(Collectors.joining("\n"));

        return "Analyze this " + snapshot.getLanguage() + " repository and provide a structured evaluation.\n\n"
            + "Repository Summary:\n"
            + "- Language: " + snapshot.getLanguage() + "\n"
            + "- Files: " + snapshot.getFilesCount() + "\n"
            + "- Size: " + snapshot.getTotalSizeKb() + "KB\n\n"
            + "Top Files:\n"
            + topFiles + "\n\n"
            + "Code Sample:\n"
            + truncate(snapshot.getFilesContent(), 2000) + "\n\n"
            + "Evaluate across 4 dimensions and provide JSON response with this exact structure:\n"
            + "{\n"
            + "  \"security\": {\n"
            + "    \"score_delta\": <integer from -20 to +20>,\n"
            + "    \"gaps\": \"<string: specific security issues or positive findings>\"\n"
            + "  },\n"
            + "  \"database\": {\n"
            + "    \"score_delta\": <integer from -20 to +20>,\n"
            + "    \"gaps\": \"<string: database design patterns or issues>\"\n"
            + "  },\n"
            + "  \"architecture\": {\n"
            + "    \"score_delta\": <integer from -20 to +20>,\n"
            + "    \"gaps\": \"<string: architectural patterns or concerns>\"\n"
            + "  },\n"
            + "  \"code_quality\": {\n"
            + "    \"score_delta\": <integer from -20 to +20>,\n"
            + "    \"gaps\": \"<string: code quality observations>\"\n"
            + "  }\n"
            + "}\n\n"
            + "Rules:\n"
            + "- score_delta: -20 (critical issues) to +20 (excellent implementation)\n"
            + "- gaps: Specific, actionable observations (1-2 sentences max per axis)\n"
            + "- Return ONLY valid JSON, no markdown code blocks or explanations";
    }

    private String callClaudeAPI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", llmApiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmModel);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("messages", new Object[]{
            Map.of(
                "role", "user",
                "content", prompt
            )
        });

        HttpEntity<String> request = new HttpEntity<>(
            objectMapper.writeValueAsString(requestBody),
            headers
        );

        log.debug("Calling Claude API: {} with model {}", llmEndpoint, llmModel);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                llmEndpoint,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return extractTextFromResponse(response.getBody());
            } else {
                log.warn("LLM API returned non-2xx status: {}", response.getStatusCode());
                throw new Exception("LLM API error: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.warn("LLM API request failed: {}", e.getMessage());
            throw e;
        }
    }

    private String extractTextFromResponse(String apiResponse) throws Exception {
        JsonNode root = objectMapper.readTree(apiResponse);
        JsonNode content = root.get("content");

        if (content != null && content.isArray() && content.size() > 0) {
            return content.get(0).get("text").asText();
        }

        log.warn("Unexpected LLM API response structure");
        throw new Exception("Invalid LLM response format");
    }

    private LLMAnalysisResult parseLLMResponse(String responseText) throws Exception {
        // Extract JSON from response (may contain extra text)
        String jsonText = extractJSON(responseText);

        JsonNode root = objectMapper.readTree(jsonText);

        Map<String, Integer> scoreDeltaByAxis = new HashMap<>();
        Map<String, String> gapsByAxis = new HashMap<>();

        // Parse each axis
        String[] axes = {"security", "database", "architecture", "code_quality"};
        String[] displayNames = {"Security", "Database", "Architecture", "Code Quality"};

        for (int i = 0; i < axes.length; i++) {
            String axis = axes[i];
            String displayName = displayNames[i];

            if (root.has(axis)) {
                JsonNode axisNode = root.get(axis);
                int scoreDelta = clampDelta(axisNode.get("score_delta").asInt(0));
                String gaps = axisNode.get("gaps").asText("No additional analysis");

                scoreDeltaByAxis.put(displayName, scoreDelta);
                gapsByAxis.put(displayName, gaps);
            } else {
                // Missing axis in response — use defaults
                scoreDeltaByAxis.put(displayName, 0);
                gapsByAxis.put(displayName, "No analysis available");
            }
        }

        log.debug("LLM analysis parsed: {} deltas extracted", scoreDeltaByAxis.size());
        return new LLMAnalysisResult(scoreDeltaByAxis, gapsByAxis);
    }

    private String extractJSON(String text) {
        // Find first { and last }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }

        // If no JSON found, return the text as-is and let parsing fail with clear error
        return text;
    }

    private int clampDelta(int delta) {
        // Clamp score delta to ±20
        return Math.max(-20, Math.min(20, delta));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
