package com.ainexus.service.impl;

import com.ainexus.service.ObservabilityMetricsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ObservabilityMetricsServiceImpl implements ObservabilityMetricsService {

    private final AtomicLong totalAiRequests = new AtomicLong(0);
    private final AtomicLong successfulAiRequests = new AtomicLong(0);
    private final AtomicLong failedAiRequests = new AtomicLong(0);
    private final AtomicLong totalAiLatencyMs = new AtomicLong(0);

    private final AtomicLong totalRagQueries = new AtomicLong(0);
    private final AtomicLong successfulRagQueries = new AtomicLong(0);
    private final AtomicLong failedRagQueries = new AtomicLong(0);
    private final AtomicLong totalRagLatencyMs = new AtomicLong(0);

    private final AtomicLong totalWorkflowExecutions = new AtomicLong(0);
    private final AtomicLong successfulWorkflows = new AtomicLong(0);
    private final AtomicLong failedWorkflows = new AtomicLong(0);

    private final AtomicLong workflowRetries = new AtomicLong(0);
    private final AtomicLong agentReplans = new AtomicLong(0);

    private final AtomicLong clientErrors4xx = new AtomicLong(0);
    private final AtomicLong serverErrors5xx = new AtomicLong(0);

    private final LocalDateTime startedAt = LocalDateTime.now();

    @Override
    public void recordAiRequest(boolean success, long latencyMs) {
        totalAiRequests.incrementAndGet();
        if (success) {
            successfulAiRequests.incrementAndGet();
        } else {
            failedAiRequests.incrementAndGet();
        }
        totalAiLatencyMs.addAndGet(latencyMs);
    }

    @Override
    public void recordRagQuery(boolean success, long latencyMs) {
        totalRagQueries.incrementAndGet();
        if (success) {
            successfulRagQueries.incrementAndGet();
        } else {
            failedRagQueries.incrementAndGet();
        }
        totalRagLatencyMs.addAndGet(latencyMs);
    }

    @Override
    public void recordWorkflowExecution(boolean success) {
        totalWorkflowExecutions.incrementAndGet();
        if (success) {
            successfulWorkflows.incrementAndGet();
        } else {
            failedWorkflows.incrementAndGet();
        }
    }

    @Override
    public void recordWorkflowRetry() {
        workflowRetries.incrementAndGet();
    }

    @Override
    public void recordAgentReplan() {
        agentReplans.incrementAndGet();
    }

    @Override
    public void recordHttpError(int statusCode) {
        if (statusCode >= 400 && statusCode < 500) {
            clientErrors4xx.incrementAndGet();
        } else if (statusCode >= 500) {
            serverErrors5xx.incrementAndGet();
        }
    }

    @Override
    public Map<String, Object> getSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("serviceStartedAt", startedAt.toString());

        long aiTotal = totalAiRequests.get();
        snapshot.put("ai", Map.of(
                "totalRequests", aiTotal,
                "successful", successfulAiRequests.get(),
                "failed", failedAiRequests.get(),
                "avgLatencyMs", aiTotal > 0 ? (totalAiLatencyMs.get() / aiTotal) : 0
        ));

        long ragTotal = totalRagQueries.get();
        snapshot.put("rag", Map.of(
                "totalQueries", ragTotal,
                "successful", successfulRagQueries.get(),
                "failed", failedRagQueries.get(),
                "avgLatencyMs", ragTotal > 0 ? (totalRagLatencyMs.get() / ragTotal) : 0
        ));

        snapshot.put("workflows", Map.of(
                "totalExecutions", totalWorkflowExecutions.get(),
                "successful", successfulWorkflows.get(),
                "failed", failedWorkflows.get(),
                "retriesTriggered", workflowRetries.get()
        ));

        snapshot.put("agents", Map.of(
                "replanningAttempts", agentReplans.get()
        ));

        snapshot.put("errors", Map.of(
                "http4xx", clientErrors4xx.get(),
                "http5xx", serverErrors5xx.get()
        ));

        return snapshot;
    }

    @Override
    public void reset() {
        totalAiRequests.set(0);
        successfulAiRequests.set(0);
        failedAiRequests.set(0);
        totalAiLatencyMs.set(0);
        totalRagQueries.set(0);
        successfulRagQueries.set(0);
        failedRagQueries.set(0);
        totalRagLatencyMs.set(0);
        totalWorkflowExecutions.set(0);
        successfulWorkflows.set(0);
        failedWorkflows.set(0);
        workflowRetries.set(0);
        agentReplans.set(0);
        clientErrors4xx.set(0);
        serverErrors5xx.set(0);
    }
}
