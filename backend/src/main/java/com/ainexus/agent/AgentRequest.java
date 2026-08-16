package com.ainexus.agent;

import com.ainexus.entity.User;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AgentRequest(
        String query,
        AgentType targetAgent,
        Long workspaceId,
        User user,
        String traceId,
        Map<String, Object> parameters
) {
    public AgentRequest {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be null or blank");
        }
        Objects.requireNonNull(targetAgent, "Target agent type must not be null");
        Objects.requireNonNull(workspaceId, "Workspace ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        parameters = (parameters != null) ? Collections.unmodifiableMap(parameters) : Collections.emptyMap();
    }

    public static AgentRequest of(String query, AgentType targetAgent, Long workspaceId, User user) {
        return new AgentRequest(query, targetAgent, workspaceId, user, UUID.randomUUID().toString(), Collections.emptyMap());
    }
}
