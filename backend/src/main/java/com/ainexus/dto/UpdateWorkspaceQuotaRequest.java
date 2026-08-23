package com.ainexus.dto;

import jakarta.validation.constraints.Min;

public record UpdateWorkspaceQuotaRequest(
        @Min(value = 0, message = "Limit must be non-negative")
        Long maxAiRequests,

        @Min(value = 0, message = "Limit must be non-negative")
        Long maxTokens,

        @Min(value = 0, message = "Limit must be non-negative")
        Long maxDocumentProcessing,

        @Min(value = 0, message = "Limit must be non-negative")
        Long maxEmbeddings,

        @Min(value = 0, message = "Limit must be non-negative")
        Long maxVectorOperations,

        @Min(value = 0, message = "Limit must be non-negative")
        Long maxWorkflowExecutions,

        @Min(value = 0, message = "Limit must be non-negative")
        Long maxAgentExecutions
) {
}
