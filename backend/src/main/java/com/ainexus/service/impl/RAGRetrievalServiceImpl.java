package com.ainexus.service.impl;

import com.ainexus.dto.EnhancedQuery;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;
import com.ainexus.service.ContextManagementService;
import com.ainexus.service.MultiQueryRetrievalService;
import com.ainexus.service.QueryEnhancementService;
import com.ainexus.service.RAGRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class RAGRetrievalServiceImpl implements RAGRetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(RAGRetrievalServiceImpl.class);

    private final ContextManagementService contextManagementService;
    private final QueryEnhancementService queryEnhancementService;
    private final MultiQueryRetrievalService multiQueryRetrievalService;

    @Value("${app.rag.top-k:5}")
    private int defaultTopK;

    public RAGRetrievalServiceImpl(ContextManagementService contextManagementService,
                                   QueryEnhancementService queryEnhancementService,
                                   MultiQueryRetrievalService multiQueryRetrievalService) {
        this.contextManagementService = Objects.requireNonNull(contextManagementService, "ContextManagementService must not be null");
        this.queryEnhancementService = Objects.requireNonNull(queryEnhancementService, "QueryEnhancementService must not be null");
        this.multiQueryRetrievalService = Objects.requireNonNull(multiQueryRetrievalService, "MultiQueryRetrievalService must not be null");
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

        // 1. Query Enhancement / Rewriting
        EnhancedQuery enhancedQuery = queryEnhancementService.enhanceQuery(query.trim());
        String effectiveRetrievalQuery = enhancedQuery.retrievalQuery();

        logger.info("Executing RAG retrieval for workspace id: {} with effective retrieval query: '{}' (original: '{}', topK: {})",
                workspaceId, effectiveRetrievalQuery, enhancedQuery.originalQuery(), topK);

        // 2. Multi-query retrieval: variations, vector search across workspace, merging & deduplication
        List<SearchResultItem> rawResults = multiQueryRetrievalService.retrieveMultiQueryResults(
                effectiveRetrievalQuery,
                workspaceId,
                topK,
                authenticatedUser
        );

        // 3. Delegate filtering, context budget limit, and final assembly to ContextManagementService
        return contextManagementService.processAndAssembleContext(enhancedQuery.originalQuery(), workspaceId, rawResults);
    }
}
