package com.ainexus.dto;

public record RAGChunk(
        Long documentId,
        String filename,
        Integer chunkIndex,
        Double score,
        String content,
        Integer characterCount
) {}
