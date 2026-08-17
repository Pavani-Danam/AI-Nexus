package com.ainexus.service.impl;

import com.ainexus.dto.RAGContext;
import com.ainexus.dto.SearchResponse;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;
import com.ainexus.service.ContextManagementService;
import com.ainexus.service.RAGRetrievalService;
import com.ainexus.service.SemanticSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class RAGRetrievalServiceImpl implements RAGRetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(RAGRetrievalServiceImpl.class);

    private final SemanticSearchService semanticSearchService;
    private final ContextManagementService contextManagementService;

    @Value("${app.rag.top-k:5}")
    private int defaultTopK;

    public RAGRetrievalServiceImpl(SemanticSearchService semanticSearchService,
                                   ContextManagementService contextManagementService) {
        this.semanticSearchService = Objects.requireNonNull(semanticSearchService, "SemanticSearchService must not be null");
        this.contextManagementService = Objects.requireNonNull(contextManagementService, "ContextManagementService must not be null");
    }

    @Override
    public RAGContext retrieveAndAssembleContext(String query, Long workspaceId, Integer requestedTopK, User authenticatedUser) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("RAG query must not be null or blank.");
        }
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null.");
        }

        int topK = (requestedTopK != null && requestedTopK > 0) ? requestedTopK : defaultTopK;

        logger.info("Executing RAG retrieval for workspace id: {} with query: '{}' (topK: {})", workspaceId, query.trim(), topK);

        // 1. Retrieve authorized vector results from SemanticSearchService
        SearchResponse searchResponse = semanticSearchService.search(query.trim(), workspaceId, topK, authenticatedUser);

        List<SearchResultItem> rawResults = (searchResponse != null && searchResponse.results() != null)
                ? searchResponse.results()
                : Collections.emptyList();

        // 2. Delegate filtering, deduplication, ordering, and context limits to ContextManagementService
        return contextManagementService.processAndAssembleContext(query.trim(), workspaceId, rawResults);
    }
}
