package com.ainexus.agent;

import com.ainexus.dto.RAGChunk;
import com.ainexus.entity.User;

import java.util.*;

public class AgentContext {

    private final String traceId;
    private final Long workspaceId;
    private final User user;
    private final Map<String, Object> metadata;
    private final List<RAGChunk> retrievedChunks;
    private final Map<String, Object> intermediateResults;

    public AgentContext(String traceId, Long workspaceId, User user) {
        this.traceId = (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
        this.workspaceId = Objects.requireNonNull(workspaceId, "Workspace ID must not be null");
        this.user = Objects.requireNonNull(user, "User must not be null");
        this.metadata = new HashMap<>();
        this.retrievedChunks = new ArrayList<>();
        this.intermediateResults = new HashMap<>();
    }

    public String getTraceId() {
        return traceId;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public User getUser() {
        return user;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public void setMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public List<RAGChunk> getRetrievedChunks() {
        return Collections.unmodifiableList(retrievedChunks);
    }

    public void addRetrievedChunks(List<RAGChunk> chunks) {
        if (chunks != null) {
            this.retrievedChunks.addAll(chunks);
        }
    }

    public Map<String, Object> getIntermediateResults() {
        return Collections.unmodifiableMap(intermediateResults);
    }

    public void setIntermediateResult(String key, Object value) {
        this.intermediateResults.put(key, value);
    }
}
