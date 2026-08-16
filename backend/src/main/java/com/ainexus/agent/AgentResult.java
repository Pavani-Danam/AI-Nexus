package com.ainexus.agent;

import com.ainexus.dto.RAGCitation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record AgentResult(
        boolean success,
        AgentType agentType,
        String traceId,
        String output,
        List<RAGCitation> citations,
        Map<String, Object> metadata,
        String errorMessage
) {
    public AgentResult {
        citations = (citations != null) ? Collections.unmodifiableList(citations) : Collections.emptyList();
        metadata = (metadata != null) ? Collections.unmodifiableMap(metadata) : Collections.emptyMap();
    }

    public static AgentResult success(AgentType agentType, String traceId, String output, List<RAGCitation> citations, Map<String, Object> metadata) {
        return new AgentResult(true, agentType, traceId, output, citations, metadata, null);
    }

    public static AgentResult failure(AgentType agentType, String traceId, String errorMessage) {
        return new AgentResult(false, agentType, traceId, null, Collections.emptyList(), Collections.emptyMap(), errorMessage);
    }
}
