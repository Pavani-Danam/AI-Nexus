package com.ainexus.dto;

import java.util.List;

public record AgentPlan(
        String planId,
        String originalQuery,
        Long workspaceId,
        List<AgentTask> tasks,
        String reasoning,
        boolean isComplex
) {
    public AgentPlan {
        tasks = tasks != null ? List.copyOf(tasks) : List.of();
    }

    public AgentPlan(String planId, String originalQuery, Long workspaceId, List<AgentTask> tasks) {
        this(planId, originalQuery, workspaceId, tasks, null, tasks != null && tasks.size() > 1);
    }
}
