package com.ainexus.dto;

import java.util.List;

public record AgentTask(
        String id,
        AgentTaskType type,
        String description,
        List<String> dependsOn,
        AgentTaskStatus status
) {
    public AgentTask {
        dependsOn = dependsOn != null ? List.copyOf(dependsOn) : List.of();
        if (status == null) {
            status = AgentTaskStatus.PENDING;
        }
    }

    public AgentTask(String id, AgentTaskType type, String description, List<String> dependsOn) {
        this(id, type, description, dependsOn, AgentTaskStatus.PENDING);
    }
}
