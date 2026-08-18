package com.ainexus.service;

import com.ainexus.dto.SearchResultItem;

import java.util.List;

/**
 * Service responsible for deterministic reranking of candidate retrieval chunks
 * before context budget management and prompt assembly.
 */
public interface RerankingService {

    /**
     * Reranks candidate search results deterministically using similarity score,
     * lexical query coverage, and exact keyword alignment.
     *
     * @param query the search/retrieval query
     * @param candidates candidate search result items
     * @return ordered and bounded list of SearchResultItem
     */
    List<SearchResultItem> rerank(String query, List<SearchResultItem> candidates);
}
