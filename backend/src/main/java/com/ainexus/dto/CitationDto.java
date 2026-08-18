package com.ainexus.dto;

public record CitationDto(
        Long citationId,
        Long chunkId,
        Long documentId,
        String filename,
        String snippet,
        Double score
) {}
