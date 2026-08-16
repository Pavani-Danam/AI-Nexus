package com.ainexus.dto;

import java.util.List;

public record RAGResponse(
        String answer,
        String query,
        Long workspaceId,
        List<RAGCitation> citations,
        List<RAGChunk> sources,
        boolean hasContext
) {
    public RAGResponse(String answer, String query, Long workspaceId, List<RAGCitation> citations, List<RAGChunk> sources, boolean hasContext) {
        this.answer = answer;
        this.query = query;
        this.workspaceId = workspaceId;
        this.citations = citations != null ? List.copyOf(citations) : List.of();
        this.sources = sources != null ? List.copyOf(sources) : List.of();
        this.hasContext = hasContext;
    }
}
