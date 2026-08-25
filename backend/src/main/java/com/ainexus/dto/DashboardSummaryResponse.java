package com.ainexus.dto;

public record DashboardSummaryResponse(
    long totalDocuments,
    long indexedDocuments,
    long processingDocuments,
    long failedDocuments,
    long totalWorkspaces,
    long totalConversations,
    long totalExecutions,
    long vectorEmbeddingsCount
) {}
