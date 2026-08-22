package com.ainexus.dto;

import java.util.List;
import java.util.Map;

public record AgentExecutionResult(
        String executionId,
        String planId,
        PlanExecutionStatus status,
        String finalOutput,
        List<AgentTaskResult> taskResults,
        Map<String, String> outputsByTaskId
) {
    public AgentExecutionResult {
        taskResults = taskResults != null ? List.copyOf(taskResults) : List.of();
        outputsByTaskId = outputsByTaskId != null ? Map.copyOf(outputsByTaskId) : Map.of();
    }
}
