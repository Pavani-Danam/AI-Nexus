package com.ainexus.dto;

public record RAGCitation(
        Long documentId,
        String filename,
        Integer chunkIndex,
        Double similarityScore,
        String sourceId,
        String snippet
) {
    public static RAGCitation fromChunk(RAGChunk chunk) {
        if (chunk == null) {
            return null;
        }
        String sourceId = "doc-" + chunk.documentId() + "-chunk-" + chunk.chunkIndex();
        String snippet = chunk.content() != null && chunk.content().length() > 200
                ? chunk.content().substring(0, 200).trim() + "..."
                : (chunk.content() != null ? chunk.content().trim() : "");

        return new RAGCitation(
                chunk.documentId(),
                chunk.filename(),
                chunk.chunkIndex(),
                chunk.score(),
                sourceId,
                snippet
        );
    }
}
