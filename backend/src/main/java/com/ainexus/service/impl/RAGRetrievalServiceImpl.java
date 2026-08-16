package com.ainexus.service.impl;

import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.SearchResponse;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;
import com.ainexus.service.RAGRetrievalService;
import com.ainexus.service.SemanticSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RAGRetrievalServiceImpl implements RAGRetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(RAGRetrievalServiceImpl.class);

    private final SemanticSearchService semanticSearchService;

    @Value("${app.rag.top-k:5}")
    private int defaultTopK;

    @Value("${app.rag.min-relevance-score:0.35}")
    private double minRelevanceScore;

    @Value("${app.rag.max-context-characters:8000}")
    private int maxContextCharacters;

    public RAGRetrievalServiceImpl(SemanticSearchService semanticSearchService) {
        this.semanticSearchService = semanticSearchService;
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

        // 1. Reuse existing authorized SemanticSearchService
        SearchResponse searchResponse = semanticSearchService.search(query.trim(), workspaceId, topK, authenticatedUser);

        if (searchResponse == null || searchResponse.results() == null || searchResponse.results().isEmpty()) {
            logger.info("No matching search results returned for RAG retrieval in workspace id: {}", workspaceId);
            return RAGContext.empty(query.trim(), workspaceId);
        }

        // 2. Filter by minimum relevance score & deduplicate
        List<RAGChunk> filteredChunks = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (SearchResultItem item : searchResponse.results()) {
            if (item.score() == null || item.score() < minRelevanceScore) {
                logger.debug("Skipping chunk below min score {}: score={}", minRelevanceScore, item.score());
                continue;
            }

            String chunkKey = (item.documentId() != null ? item.documentId() : "doc") + ":" +
                             (item.chunkIndex() != null ? item.chunkIndex() : "chk") + ":" +
                             (item.vectorId() != null ? item.vectorId() : "");

            if (seenKeys.add(chunkKey)) {
                filteredChunks.add(new RAGChunk(
                        item.documentId(),
                        item.filename(),
                        item.chunkIndex(),
                        item.score(),
                        item.content() != null ? item.content() : "",
                        item.characterCount() != null ? item.characterCount() : (item.content() != null ? item.content().length() : 0)
                ));
            }
        }

        if (filteredChunks.isEmpty()) {
            logger.info("All retrieved results fell below relevance threshold {} for workspace id: {}", minRelevanceScore, workspaceId);
            return RAGContext.empty(query.trim(), workspaceId);
        }

        // 3. Assemble structured context under maxContextCharacters budget
        StringBuilder contextBuilder = new StringBuilder();
        List<RAGChunk> finalChunks = new ArrayList<>();
        int currentCharacters = 0;

        for (int i = 0; i < filteredChunks.size(); i++) {
            RAGChunk chunk = filteredChunks.get(i);
            String chunkHeader = String.format("[Source: %s, Chunk: %d, Score: %.3f]\n",
                    chunk.filename() != null ? chunk.filename() : "Unknown Document",
                    chunk.chunkIndex() != null ? chunk.chunkIndex() + 1 : 1,
                    chunk.score() != null ? chunk.score() : 0.0);

            String formattedChunk = chunkHeader + chunk.content().trim() + "\n\n";
            int chunkLength = formattedChunk.length();

            if (currentCharacters + chunkLength > maxContextCharacters) {
                if (currentCharacters == 0) {
                    // Single chunk exceeds budget: safely truncate to max budget
                    String truncated = formattedChunk.substring(0, maxContextCharacters);
                    contextBuilder.append(truncated);
                    finalChunks.add(chunk);
                    logger.warn("First chunk exceeded total context budget. Truncated to {} chars.", maxContextCharacters);
                } else {
                    logger.warn("Context character budget reached ({} + {} > {}). Truncating further chunks.",
                            currentCharacters, chunkLength, maxContextCharacters);
                }
                break;
            }

            contextBuilder.append(formattedChunk);
            currentCharacters += chunkLength;
            finalChunks.add(chunk);
        }

        String assembledContext = contextBuilder.toString().trim();
        logger.info("Assembled RAG context containing {} chunks ({} total characters) for workspace id: {}",
                finalChunks.size(), assembledContext.length(), workspaceId);

        return new RAGContext(query.trim(), workspaceId, finalChunks, assembledContext, assembledContext.length());
    }
}
