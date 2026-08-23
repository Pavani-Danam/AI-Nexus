package com.ainexus.dto;

import com.ainexus.entity.WorkspaceQuota;

import java.time.LocalDateTime;

public record WorkspaceQuotaResponse(
        Long id,
        Long workspaceId,
        String workspaceName,

        // Limits
        long maxAiRequests,
        long maxTokens,
        long maxDocumentProcessing,
        long maxEmbeddings,
        long maxVectorOperations,
        long maxWorkflowExecutions,
        long maxAgentExecutions,

        // Usage
        long usedAiRequests,
        long usedTokens,
        long usedDocumentProcessing,
        long usedEmbeddings,
        long usedVectorOperations,
        long usedWorkflowExecutions,
        long usedAgentExecutions,

        // Remaining
        long remainingAiRequests,
        long remainingTokens,
        long remainingDocumentProcessing,
        long remainingEmbeddings,
        long remainingVectorOperations,
        long remainingWorkflowExecutions,
        long remainingAgentExecutions,

        LocalDateTime updatedAt
) {
    public static WorkspaceQuotaResponse fromEntity(WorkspaceQuota quota) {
        return new WorkspaceQuotaResponse(
                quota.getId(),
                quota.getWorkspace() != null ? quota.getWorkspace().getId() : null,
                quota.getWorkspace() != null ? quota.getWorkspace().getName() : "Unknown",

                quota.getMaxAiRequests(),
                quota.getMaxTokens(),
                quota.getMaxDocumentProcessing(),
                quota.getMaxEmbeddings(),
                quota.getMaxVectorOperations(),
                quota.getMaxWorkflowExecutions(),
                quota.getMaxAgentExecutions(),

                quota.getUsedAiRequests(),
                quota.getUsedTokens(),
                quota.getUsedDocumentProcessing(),
                quota.getUsedEmbeddings(),
                quota.getUsedVectorOperations(),
                quota.getUsedWorkflowExecutions(),
                quota.getUsedAgentExecutions(),

                Math.max(0, quota.getMaxAiRequests() - quota.getUsedAiRequests()),
                Math.max(0, quota.getMaxTokens() - quota.getUsedTokens()),
                Math.max(0, quota.getMaxDocumentProcessing() - quota.getUsedDocumentProcessing()),
                Math.max(0, quota.getMaxEmbeddings() - quota.getUsedEmbeddings()),
                Math.max(0, quota.getMaxVectorOperations() - quota.getUsedVectorOperations()),
                Math.max(0, quota.getMaxWorkflowExecutions() - quota.getUsedWorkflowExecutions()),
                Math.max(0, quota.getMaxAgentExecutions() - quota.getUsedAgentExecutions()),

                quota.getUpdatedAt()
        );
    }
}
