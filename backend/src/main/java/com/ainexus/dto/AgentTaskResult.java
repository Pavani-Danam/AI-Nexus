package com.ainexus.dto;

import java.time.LocalDateTime;

public record AgentTaskResult(
        String taskId,
        AgentTaskType taskType,
        AgentTaskStatus status,
        String output,
        String errorMessage,
        LocalDateTime executedAt
) {
    public AgentTaskResult(String taskId, AgentTaskType taskType, AgentTaskStatus status, String output) {
        this(taskId, taskType, status, output, null, LocalDateTime.now());
    }

    public static AgentTaskResult success(String taskId, AgentTaskType taskType, String output) {
        return new AgentTaskResult(taskId, taskType, AgentTaskStatus.COMPLETED, output, null, LocalDateTime.now());
    }

    public static AgentTaskResult failure(String taskId, AgentTaskType taskType, String errorMessage) {
        return new AgentTaskResult(taskId, taskType, AgentTaskStatus.FAILED, null, errorMessage, LocalDateTime.now());
    }

    public static AgentTaskResult skipped(String taskId, AgentTaskType taskType, String reason) {
        return new AgentTaskResult(taskId, taskType, AgentTaskStatus.SKIPPED, null, reason, LocalDateTime.now());
    }
}
