package com.learnhub.assessment.scoring;

import com.learnhub.assessment.analysis.StaticAnalysisResult;
import com.learnhub.assessment.lm.LLMAnalysisResult;
import com.learnhub.github.entity.RepositorySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CompetencyScoringAlgorithm {

    public RadarSeriesDto computeScores(
            StaticAnalysisResult staticResult,
            LLMAnalysisResult llmResult,
            RepositorySnapshot snapshot) {

        RadarSeriesDto radar = new RadarSeriesDto();
        radar.setLanguage(snapshot.getLanguage());

        List<RadarSeriesDto.RadarSeries> seriesList = new ArrayList<>();

        // Axis 1: SECURITY
        seriesList.add(computeAxis("Security",
            staticResult.getFindings().stream()
                .filter(f -> f.axis().equals("Security"))
                .collect(Collectors.toList()),
            llmResult.gapsByAxis().getOrDefault("Security", "No gaps detected"),
            llmResult.scoreDeltaByAxis().getOrDefault("Security", 0)
        ));

        // Axis 2: DATABASE
        seriesList.add(computeAxis("Database",
            staticResult.getFindings().stream()
                .filter(f -> f.axis().equals("Database"))
                .collect(Collectors.toList()),
            llmResult.gapsByAxis().getOrDefault("Database", "No gaps detected"),
            llmResult.scoreDeltaByAxis().getOrDefault("Database", 0)
        ));

        // Axis 3: ARCHITECTURE
        seriesList.add(computeAxis("Architecture",
            staticResult.getFindings().stream()
                .filter(f -> f.axis().equals("Architecture"))
                .collect(Collectors.toList()),
            llmResult.gapsByAxis().getOrDefault("Architecture", "No gaps detected"),
            llmResult.scoreDeltaByAxis().getOrDefault("Architecture", 0)
        ));

        // Axis 4: CODE QUALITY
        seriesList.add(computeAxis("Code Quality",
            staticResult.getFindings().stream()
                .filter(f -> f.axis().equals("Code Quality"))
                .collect(Collectors.toList()),
            llmResult.gapsByAxis().getOrDefault("Code Quality", "No gaps detected"),
            llmResult.scoreDeltaByAxis().getOrDefault("Code Quality", 0)
        ));

        radar.setSeriesList(seriesList);
        return radar;
    }

    private RadarSeriesDto.RadarSeries computeAxis(
            String axisName,
            List<StaticAnalysisResult.CodeFinding> findings,
            String llmGap,
            Integer llmScoreDelta) {

        // Start at 70 (baseline for decent code)
        int score = 70;

        // Deduct points for static findings (integer arithmetic to avoid NaN)
        for (StaticAnalysisResult.CodeFinding finding : findings) {
            int deduction = switch(finding.severity()) {
                case "CRITICAL" -> 20;
                case "HIGH" -> 15;
                case "MEDIUM" -> 10;
                case "LOW" -> 5;
                default -> 0;
            };
            score -= deduction;
        }

        // LLM fine-tuning (±15 points max)
        if (llmScoreDelta != null) {
            score += llmScoreDelta;
        }

        // Clamp to valid range [0, 100] - critical to avoid NaN
        score = Math.max(0, Math.min(100, score));

        // Determine level badge
        String level;
        if (score >= 80) {
            level = "EXCELLENT";
        } else if (score >= 60) {
            level = "GOOD";
        } else if (score >= 40) {
            level = "FAIR";
        } else {
            level = "POOR";
        }

        // Construct gap analysis text from findings + LLM
        String gapAnalysis = buildGapAnalysis(findings, llmGap);

        return new RadarSeriesDto.RadarSeries(
            axisName,
            score,
            axisName,
            level,
            gapAnalysis
        );
    }

    private String buildGapAnalysis(List<StaticAnalysisResult.CodeFinding> findings, String llmGap) {
        StringBuilder sb = new StringBuilder();

        if (!findings.isEmpty()) {
            sb.append("Static Analysis Findings:\n");
            for (StaticAnalysisResult.CodeFinding f : findings.stream()
                    .sorted(Comparator.comparing(StaticAnalysisResult.CodeFinding::severity)
                        .reversed())
                    .limit(3)
                    .collect(Collectors.toList())) {
                sb.append(String.format("- [%s] %s (at %s)\n", f.severity(), f.description(), f.location()));
            }
        }

        if (llmGap != null && !llmGap.isEmpty() && !llmGap.equals("No gaps detected")) {
            if (!findings.isEmpty()) {
                sb.append("\n");
            }
            sb.append("Deep Analysis:\n").append(llmGap);
        }

        return sb.toString().trim();
    }
}
