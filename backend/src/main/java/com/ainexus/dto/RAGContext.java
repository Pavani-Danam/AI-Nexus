package com.ainexus.dto;

import java.util.Collections;
import java.util.List;

public record RAGContext(
        String query,
        Long workspaceId,
        List<RAGChunk> chunks,
        String assembledContext,
        int totalCharacters
) {
    public static RAGContext empty(String query, Long workspaceId) {
        return new RAGContext(query, workspaceId, Collections.emptyList(), "", 0);
    }
}
