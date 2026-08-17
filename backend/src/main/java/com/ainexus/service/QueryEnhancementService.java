package com.ainexus.service;

import com.ainexus.dto.EnhancedQuery;

/**
 * Service responsible for analyzing and enhancing user queries for vector search retrieval.
 * Preserves clear queries, expands vague queries into precise retrieval representations,
 * enforces safety/length limits, and provides graceful fallback to the original query.
 */
public interface QueryEnhancementService {

    /**
     * Enhances a user query into an optimized retrieval query.
     *
     * @param originalQuery the raw query submitted by the user
     * @return an EnhancedQuery containing the original query, the retrieval query, and rewrite status
     */
    EnhancedQuery enhanceQuery(String originalQuery);
}
