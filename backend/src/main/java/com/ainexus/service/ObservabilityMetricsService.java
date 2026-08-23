package com.ainexus.service;

import java.util.Map;

public interface ObservabilityMetricsService {

    void recordAiRequest(boolean success, long latencyMs);

    void recordRagQuery(boolean success, long latencyMs);

    void recordWorkflowExecution(boolean success);

    void recordWorkflowRetry();

    void recordAgentReplan();

    void recordHttpError(int statusCode);

    Map<String, Object> getSnapshot();

    void reset();
}
