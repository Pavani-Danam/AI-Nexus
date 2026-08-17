package com.ainexus.service;

import com.ainexus.dto.RAGContext;
import com.ainexus.dto.SearchResultItem;

import java.util.List;

/**
 * Service responsible for processing, filtering, deduplicating, ordering,
 * and assembling retrieved document chunks into clean, budget-constrained context.
 */
public interface ContextManagementService {

    /**
     * Processes raw search items, filters by relevance score, eliminates duplicate content,
     * preserves source metadata, and formats context within character budget limits.
     *
     * @param query the search/RAG query
     * @param workspaceId the workspace identifier
     * @param rawResults the raw retrieved search results from vector search
     * @return a structured RAGContext containing cleaned chunks and formatted context text
     */
    RAGContext processAndAssembleContext(String query, Long workspaceId, List<SearchResultItem> rawResults);
}
