package com.ainexus.dto;

public record RecordUsageRequest(
        long aiRequests,
        long tokens,
        long documentProcessing,
        long embeddings,
        long vectorOperations,
        long workflowExecutions,
        long agentExecutions
) {
}
