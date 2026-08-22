package com.ainexus.dto;

import java.time.LocalDateTime;

public record AgentTaskResult(
        String taskId,
        AgentTaskType taskType,
        AgentTaskStatus status,
        String output,
        String errorMessage,
        FailureCategory failureCategory,
        int attempts,
        LocalDateTime executedAt
) {
    public AgentTaskResult(String taskId, AgentTaskType taskType, AgentTaskStatus status, String output) {
        this(taskId, taskType, status, output, null, null, 1, LocalDateTime.now());
    }

    public static AgentTaskResult success(String taskId, AgentTaskType taskType, String output, int attempts) {
        return new AgentTaskResult(taskId, taskType, AgentTaskStatus.COMPLETED, output, null, null, attempts, LocalDateTime.now());
    }

    public static AgentTaskResult failure(String taskId, AgentTaskType taskType, String errorMessage, FailureCategory category, int attempts) {
        return new AgentTaskResult(taskId, taskType, AgentTaskStatus.FAILED, null, errorMessage, category, attempts, LocalDateTime.now());
    }

    public static AgentTaskResult skipped(String taskId, AgentTaskType taskType, String reason) {
        return new AgentTaskResult(taskId, taskType, AgentTaskStatus.SKIPPED, null, reason, FailureCategory.DEPENDENCY_FAILURE, 0, LocalDateTime.now());
    }
}
