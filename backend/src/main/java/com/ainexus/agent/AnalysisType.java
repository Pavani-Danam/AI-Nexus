package com.ainexus.agent;

public enum AnalysisType {
    SUMMARY("Summarizes key points and findings from retrieved documents"),
    COMPARISON("Compares, contrasts, and identifies differences/similarities across retrieved documents"),
    QUESTION_ANALYSIS("Answers deep analytical questions strictly grounded in retrieved document context");

    private final String description;

    AnalysisType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static AnalysisType fromString(String value) {
        if (value == null || value.isBlank()) {
            return QUESTION_ANALYSIS;
        }
        try {
            return AnalysisType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid analysis type: '" + value + "'. Supported types: SUMMARY, COMPARISON, QUESTION_ANALYSIS");
        }
    }
}
