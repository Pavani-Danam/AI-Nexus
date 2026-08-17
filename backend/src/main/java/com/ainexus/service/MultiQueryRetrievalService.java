package com.ainexus.service;

import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;

import java.util.List;

/**
 * Service responsible for multi-query retrieval: generating search variations,
 * executing vector retrieval across authorized workspaces, merging and deduplicating chunks,
 * and ranking results deterministically by similarity score.
 */
public interface MultiQueryRetrievalService {

    /**
     * Generates multiple search query variations, executes retrieval for each,
     * merges and deduplicates results, and returns the top ranked results.
     *
     * @param primaryRetrievalQuery the enhanced query to generate variations for
     * @param workspaceId the authorized workspace ID
     * @param topK the requested top-k chunks per query
     * @param authenticatedUser the authenticated user executing the search
     * @return merged, deduplicated, and score-ranked SearchResultItem list
     */
    List<SearchResultItem> retrieveMultiQueryResults(String primaryRetrievalQuery,
                                                     Long workspaceId,
                                                     Integer topK,
                                                     User authenticatedUser);
}
