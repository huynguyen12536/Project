package com.learnhub.assessment.analysis;

import java.util.ArrayList;
import java.util.List;

public class StaticAnalysisResult {
    private List<CodeFinding> findings = new ArrayList<>();

    public void addFinding(String axis, String description, String severity, String location) {
        findings.add(new CodeFinding(axis, description, severity, location));
    }

    public List<CodeFinding> getFindings() {
        return findings;
    }

    public record CodeFinding(String axis, String description, String severity, String location) {}
}
