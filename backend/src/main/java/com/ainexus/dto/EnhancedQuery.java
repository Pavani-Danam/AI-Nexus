package com.ainexus.dto;

import java.util.Objects;

/**
 * Encapsulates the user's original query and the enhanced retrieval query.
 */
public record EnhancedQuery(
        String originalQuery,
        String retrievalQuery,
        boolean rewritten
) {
    public EnhancedQuery {
        Objects.requireNonNull(originalQuery, "originalQuery must not be null");
        Objects.requireNonNull(retrievalQuery, "retrievalQuery must not be null");
    }

    public static EnhancedQuery unchanged(String originalQuery) {
        return new EnhancedQuery(originalQuery, originalQuery, false);
    }

    public static EnhancedQuery of(String originalQuery, String retrievalQuery) {
        boolean wasRewritten = !originalQuery.trim().equalsIgnoreCase(retrievalQuery.trim());
        return new EnhancedQuery(originalQuery, retrievalQuery, wasRewritten);
    }
}
