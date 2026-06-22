package com.learnhub.assessment.lm;

import java.util.Map;

public record LLMAnalysisResult(
    Map<String, Integer> scoreDeltaByAxis,
    Map<String, String> gapsByAxis
) {
    public static final LLMAnalysisResult DEFAULT = new LLMAnalysisResult(
        Map.of(
            "Security", 0,
            "Database", 0,
            "Architecture", 0,
            "Code Quality", 0
        ),
        Map.of(
            "Security", "No gaps detected",
            "Database", "No gaps detected",
            "Architecture", "No gaps detected",
            "Code Quality", "No gaps detected"
        )
    );
}
