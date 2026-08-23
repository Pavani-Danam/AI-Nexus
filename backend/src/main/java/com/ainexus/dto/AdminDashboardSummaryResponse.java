package com.ainexus.dto;

import java.util.Map;

public record AdminDashboardSummaryResponse(
        long totalUsers,
        long activeUsers,
        long totalWorkspaces,
        long totalDocuments,
        long totalWorkflows,
        long totalExecutions,
        long successfulExecutions,
        long failedExecutions,
        long totalAiTokensUsed,
        String systemHealthStatus,
        Map<String, Object> systemMetrics
) {
}
