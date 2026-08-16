package com.ainexus.agent;

import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.SearchResponse;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.service.SemanticSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SearchAgent implements Agent {

    private static final Logger logger = LoggerFactory.getLogger(SearchAgent.class);

    private final SemanticSearchService semanticSearchService;

    public SearchAgent(SemanticSearchService semanticSearchService) {
        this.semanticSearchService = Objects.requireNonNull(semanticSearchService, "SemanticSearchService must not be null");
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.SEARCH;
    }

    @Override
    public AgentResult execute(AgentRequest request, AgentContext context) {
        Objects.requireNonNull(request, "AgentRequest must not be null");
        Objects.requireNonNull(context, "AgentContext must not be null");

        String traceId = (request.traceId() != null && !request.traceId().isBlank())
                ? request.traceId()
                : context.getTraceId();

        logger.info("[Trace: {}] SearchAgent execution started for workspace id: {} with query: '{}'",
                traceId, request.workspaceId(), request.query());

        try {
            // Determine topK if provided in parameters, otherwise null for service defaults
            Integer topK = null;
            if (request.parameters().containsKey("topK")) {
                Object topKObj = request.parameters().get("topK");
                if (topKObj instanceof Number number) {
                    topK = number.intValue();
                }
            }

            // Perform authorized vector retrieval via existing SemanticSearchService
            SearchResponse searchResponse = semanticSearchService.search(
                    request.query(),
                    request.workspaceId(),
                    topK,
                    request.user()
            );

            List<SearchResultItem> results = (searchResponse != null && searchResponse.results() != null)
                    ? searchResponse.results()
                    : Collections.emptyList();

            // Transform into RAGChunks to preserve on AgentContext for downstream agents
            List<RAGChunk> chunks = new ArrayList<>();
            for (SearchResultItem item : results) {
                if (item != null) {
                    chunks.add(new RAGChunk(
                            item.documentId(),
                            item.filename(),
                            item.chunkIndex(),
                            item.score(),
                            item.content(),
                            item.characterCount()
                    ));
                }
            }
            context.addRetrievedChunks(chunks);

            // Record execution metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("totalResults", results.size());
            metadata.put("workspaceId", request.workspaceId());
            metadata.put("searchResults", results);

            logger.info("[Trace: {}] SearchAgent successfully retrieved {} results for workspace id: {}",
                    traceId, results.size(), request.workspaceId());

            return AgentResult.success(
                    AgentType.SEARCH,
                    traceId,
                    "Retrieved " + results.size() + " relevant document chunk(s).",
                    Collections.emptyList(),
                    metadata
            );

        } catch (IllegalArgumentException e) {
            logger.warn("[Trace: {}] SearchAgent validation error: {}", traceId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("[Trace: {}] SearchAgent execution failed: {}", traceId, e.getMessage());
            throw new AgentException("Search agent failed during retrieval: " + e.getMessage(), AgentType.SEARCH, traceId, e);
        }
    }
}
