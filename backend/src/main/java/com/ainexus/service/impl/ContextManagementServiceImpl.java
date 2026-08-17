package com.ainexus.service.impl;

import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.service.ContextManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ContextManagementServiceImpl implements ContextManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ContextManagementServiceImpl.class);

    @Value("${app.rag.min-relevance-score:${app.rag.relevance-threshold:0.35}}")
    private double minRelevanceScore;

    @Value("${app.rag.max-context-characters:8000}")
    private int maxContextCharacters;

    @Override
    public RAGContext processAndAssembleContext(String query, Long workspaceId, List<SearchResultItem> rawResults) {
        String cleanQuery = (query != null) ? query.trim() : "";
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null.");
        }

        if (rawResults == null || rawResults.isEmpty()) {
            logger.info("No raw results provided to ContextManagementService for workspace id: {}", workspaceId);
            return RAGContext.empty(cleanQuery, workspaceId);
        }

        // 1. Sort by relevance score descending (preserving primary ranking)
        List<SearchResultItem> sortedResults = new ArrayList<>(rawResults);
        sortedResults.sort((a, b) -> {
            double scoreA = (a != null && a.score() != null) ? a.score() : 0.0;
            double scoreB = (b != null && b.score() != null) ? b.score() : 0.0;
            return Double.compare(scoreB, scoreA);
        });

        // 2. Filter below threshold and deduplicate
        List<RAGChunk> filteredChunks = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (SearchResultItem item : sortedResults) {
            if (item == null) {
                continue;
            }

            if (item.score() == null || item.score() < minRelevanceScore) {
                logger.debug("Filtered out chunk below threshold {}: score={}", minRelevanceScore, item.score());
                continue;
            }

            String content = (item.content() != null) ? item.content().trim() : "";
            if (content.isEmpty()) {
                continue;
            }

            // Deduplication key: combination of document ID, chunk index, vector ID, and content hash
            String dedupKey = String.format("%s:%s:%s:%d",
                    item.documentId() != null ? item.documentId() : "doc",
                    item.chunkIndex() != null ? item.chunkIndex() : "chk",
                    item.vectorId() != null ? item.vectorId() : "",
                    content.hashCode()
            );

            if (seenKeys.add(dedupKey)) {
                filteredChunks.add(new RAGChunk(
                        item.documentId(),
                        item.filename(),
                        item.chunkIndex(),
                        item.score(),
                        content,
                        item.characterCount() != null ? item.characterCount() : content.length()
                ));
            }
        }

        if (filteredChunks.isEmpty()) {
            logger.info("All chunks filtered out by relevance threshold ({}) for workspace id: {}", minRelevanceScore, workspaceId);
            return RAGContext.empty(cleanQuery, workspaceId);
        }

        // 3. Assemble structured context respecting maxContextCharacters budget
        StringBuilder contextBuilder = new StringBuilder();
        List<RAGChunk> finalChunks = new ArrayList<>();
        int currentCharacters = 0;

        for (RAGChunk chunk : filteredChunks) {
            String chunkHeader = String.format("[Source: %s, Chunk: %d, Score: %.3f]\n",
                    chunk.filename() != null ? chunk.filename() : "Unknown Document",
                    chunk.chunkIndex() != null ? chunk.chunkIndex() + 1 : 1,
                    chunk.score() != null ? chunk.score() : 0.0);

            String formattedChunk = chunkHeader + chunk.content() + "\n\n";
            int chunkLength = formattedChunk.length();

            if (currentCharacters + chunkLength > maxContextCharacters) {
                if (currentCharacters == 0) {
                    // Single chunk exceeds entire budget - safely truncate
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
        logger.info("Context management produced {} chunks ({} total characters) for workspace id: {}",
                finalChunks.size(), assembledContext.length(), workspaceId);

        return new RAGContext(cleanQuery, workspaceId, finalChunks, assembledContext, assembledContext.length());
    }
}
