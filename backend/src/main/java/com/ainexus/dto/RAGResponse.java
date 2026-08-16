package com.ainexus.dto;

import java.util.List;

public record RAGResponse(
        String answer,
        String query,
        Long workspaceId,
        List<RAGChunk> sources,
        boolean hasContext
) {}
