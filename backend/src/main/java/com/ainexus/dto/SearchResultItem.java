package com.ainexus.dto;

public record SearchResultItem(
        Long documentId,
        String filename,
        Integer chunkIndex,
        Double score,
        String content,
        Integer characterCount,
        String fileType,
        String vectorId
) {}
