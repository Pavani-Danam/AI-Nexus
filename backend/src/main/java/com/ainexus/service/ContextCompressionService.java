package com.ainexus.service;

import com.ainexus.dto.SearchResultItem;

import java.util.List;

/**
 * Service responsible for contextual compression of retrieved document chunks.
 * Extracts and retains relevant information while filtering out irrelevant sentences
 * and preserving strict source metadata and meaning.
 */
public interface ContextCompressionService {

    /**
     * Compresses the content of candidate retrieval chunks based on relevance to the query.
     *
     * @param query the search or user query
     * @param candidateChunks reranked candidate chunks
     * @return list of compressed SearchResultItem retaining exact source metadata
     */
    List<SearchResultItem> compressContext(String query, List<SearchResultItem> candidateChunks);
}
