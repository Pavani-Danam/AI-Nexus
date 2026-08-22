package com.ainexus.dto;

public enum AgentTaskType {
    SEARCH,
    ANALYZE,
    KNOWLEDGE,
    SYNTHESIZE;

    public static AgentTaskType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SEARCH;
        }
        try {
            return AgentTaskType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
