package com.ainexus.dto;

import java.util.List;

public record WorkflowMonitoringSummaryResponse(
        long totalExecutions,
        long successfulExecutions,
        long failedExecutions,
        long pendingApprovals,
        double avgDurationMs,
        List<WorkflowExecutionResponse> recentExecutions
) {
}
