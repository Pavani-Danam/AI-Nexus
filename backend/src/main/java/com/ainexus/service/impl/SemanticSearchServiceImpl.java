package com.ainexus.service.impl;

import com.ainexus.dto.SearchResponse;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.model.vector.VectorQueryResult;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.EmbeddingService;
import com.ainexus.service.SemanticSearchService;
import com.ainexus.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private static final Logger logger = LoggerFactory.getLogger(SemanticSearchServiceImpl.class);

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Value("${app.search.default-top-k:5}")
    private int defaultTopK;

    @Value("${app.search.max-top-k:20}")
    private int maxTopK;

    @Value("${app.search.min-score:0.0}")
    private double minScore;

    @Value("${app.search.max-query-length:1000}")
    private int maxQueryLength;

    public SemanticSearchServiceImpl(EmbeddingService embeddingService,
                                    VectorStoreService vectorStoreService,
                                    WorkspaceRepository workspaceRepository,
                                    WorkspaceMemberRepository workspaceMemberRepository) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Override
    public SearchResponse search(String query, Long workspaceId, Integer requestedTopK, User authenticatedUser) {
        // 1. Query validation
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query must not be null or blank.");
        }

        String cleanedQuery = query.trim();
        if (cleanedQuery.length() > maxQueryLength) {
            throw new IllegalArgumentException("Search query exceeds maximum allowed length of " + maxQueryLength + " characters.");
        }

        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null.");
        }

        if (authenticatedUser == null) {
            throw new AccessDeniedException("User must be authenticated to perform search.");
        }

        // 2. Validate workspace existence and user authorization
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(authenticatedUser.getId());
        boolean isMember = workspaceMemberRepository.findByWorkspaceAndUser(workspace, authenticatedUser).isPresent();

        if (!isOwner && !isMember) {
            throw new AccessDeniedException("User does not have access to workspace id: " + workspaceId);
        }

        // 3. Resolve and sanitize topK
        int topK = (requestedTopK != null && requestedTopK > 0) ? requestedTopK : defaultTopK;
        if (topK > maxTopK) {
            topK = maxTopK;
        }

        // 4. Generate query embedding
        logger.info("Generating query embedding for workspace id: {} (topK: {})", workspaceId, topK);
        List<Float> queryVector = embeddingService.generateEmbedding(cleanedQuery);

        // 5. Query Pinecone vector store with workspace namespace isolation
        logger.info("Querying vector store for workspace id: {}", workspaceId);
        List<VectorQueryResult> queryResults = vectorStoreService.query(workspaceId, queryVector, topK);

        if (queryResults == null || queryResults.isEmpty()) {
            logger.info("No vector search results found for workspace id: {}", workspaceId);
            return new SearchResponse(cleanedQuery, workspaceId, 0, Collections.emptyList());
        }

        // 6. Filter by minScore, map to DTO, and sort descending by score
        List<SearchResultItem> searchItems = queryResults.stream()
                .filter(res -> res.score() != null && res.score() >= minScore)
                .sorted(Comparator.comparing(VectorQueryResult::score, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToSearchResultItem)
                .toList();

        logger.info("Search returned {} relevant chunks for workspace id: {}", searchItems.size(), workspaceId);
        return new SearchResponse(cleanedQuery, workspaceId, searchItems.size(), searchItems);
    }

    private SearchResultItem mapToSearchResultItem(VectorQueryResult result) {
        Map<String, Object> metadata = result.metadata() != null ? result.metadata() : Collections.emptyMap();

        Long documentId = null;
        Object docIdObj = metadata.get("documentId");
        if (docIdObj instanceof Number num) {
            documentId = num.longValue();
        } else if (docIdObj instanceof String str) {
            try {
                documentId = Long.parseLong(str);
            } catch (NumberFormatException ignored) {}
        }

        Integer chunkIndex = null;
        Object chkIdxObj = metadata.get("chunkIndex");
        if (chkIdxObj instanceof Number num) {
            chunkIndex = num.intValue();
        } else if (chkIdxObj instanceof String str) {
            try {
                chunkIndex = Integer.parseInt(str);
            } catch (NumberFormatException ignored) {}
        }

        Integer characterCount = null;
        Object charCountObj = metadata.get("characterCount");
        if (charCountObj instanceof Number num) {
            characterCount = num.intValue();
        } else if (charCountObj instanceof String str) {
            try {
                characterCount = Integer.parseInt(str);
            } catch (NumberFormatException ignored) {}
        }

        String filename = metadata.get("originalFilename") != null ? metadata.get("originalFilename").toString() : "";
        String fileType = metadata.get("fileType") != null ? metadata.get("fileType").toString() : "";
        String content = metadata.get("content") != null ? metadata.get("content").toString() : "";

        return new SearchResultItem(
                documentId,
                filename,
                chunkIndex,
                result.score(),
                content,
                characterCount,
                fileType,
                result.id()
        );
    }
}
