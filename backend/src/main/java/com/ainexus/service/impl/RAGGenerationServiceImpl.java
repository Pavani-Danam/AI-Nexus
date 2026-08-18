package com.ainexus.service.impl;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.dto.RAGCitation;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.RAGPrompt;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;
import com.ainexus.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class RAGGenerationServiceImpl implements RAGGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(RAGGenerationServiceImpl.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.generation.model:gemini-1.5-flash}")
    private String generationModel;

    private final RAGRetrievalService ragRetrievalService;
    private final RAGPromptBuilder ragPromptBuilder;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private SemanticCacheService semanticCacheService;
    private ConversationMemoryService conversationMemoryService;
    private ConversationQueryRewriteService conversationQueryRewriteService;
    private MemoryRetrievalService memoryRetrievalService;

    public RAGGenerationServiceImpl(RAGRetrievalService ragRetrievalService, RAGPromptBuilder ragPromptBuilder) {
        this.ragRetrievalService = ragRetrievalService;
        this.ragPromptBuilder = ragPromptBuilder;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Autowired(required = false)
    public void setSemanticCacheService(SemanticCacheService semanticCacheService) {
        this.semanticCacheService = semanticCacheService;
    }

    @Autowired(required = false)
    public void setConversationMemoryService(ConversationMemoryService conversationMemoryService) {
        this.conversationMemoryService = conversationMemoryService;
    }

    @Autowired(required = false)
    public void setConversationQueryRewriteService(ConversationQueryRewriteService conversationQueryRewriteService) {
        this.conversationQueryRewriteService = conversationQueryRewriteService;
    }

    @Autowired(required = false)
    public void setMemoryRetrievalService(MemoryRetrievalService memoryRetrievalService) {
        this.memoryRetrievalService = memoryRetrievalService;
    }

    @Override
    public RAGResponse generateAnswer(String query, Long workspaceId, Integer topK, User authenticatedUser) {
        return generateAnswer(query, workspaceId, topK, null, authenticatedUser);
    }

    @Override
    public RAGResponse generateAnswer(String query, Long workspaceId, Integer topK, Long conversationId, User authenticatedUser) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query must not be empty or blank.");
        }
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null.");
        }

        int effectiveTopK = (topK != null && topK > 0) ? topK : 5;
        String cleanQuery = query.trim();
        logger.info("Starting RAG generation for workspace id: {} with query: '{}' (conversationId: {})",
                workspaceId, cleanQuery, conversationId);

        // 1. Retrieve Conversation Context with Graceful Fallback
        ConversationMemory conversationMemory = null;
        if (conversationId != null) {
            try {
                if (memoryRetrievalService != null) {
                    conversationMemory = memoryRetrievalService.retrieveRelevantMemory(cleanQuery, conversationId, workspaceId, authenticatedUser);
                } else if (conversationMemoryService != null) {
                    conversationMemory = conversationMemoryService.getMemory(conversationId, workspaceId, authenticatedUser);
                }
            } catch (Exception e) {
                logger.warn("Memory retrieval failed for conversation {}: {}. Proceeding without memory context.", conversationId, e.getMessage());
                conversationMemory = null;
            }
        }

        // 2. Query Rewriting for follow-up conversational turns
        String effectiveQuery = cleanQuery;
        if (conversationMemory != null && conversationMemory.hasHistory() && conversationQueryRewriteService != null) {
            try {
                effectiveQuery = conversationQueryRewriteService.rewriteToStandaloneQuery(cleanQuery, conversationMemory, workspaceId, authenticatedUser);
            } catch (Exception e) {
                logger.warn("Query rewriting failed: {}. Using original query.", e.getMessage());
                effectiveQuery = cleanQuery;
            }
        }

        // 3. Semantic Cache Lookup (bypassed if conversation context is active)
        boolean hasMemoryHistory = (conversationMemory != null && conversationMemory.hasHistory());
        if (!hasMemoryHistory && semanticCacheService != null) {
            Optional<RAGResponse> cached = semanticCacheService.lookup(effectiveQuery, workspaceId, authenticatedUser);
            if (cached.isPresent()) {
                logger.info("Returning cached response for workspace id: {} and query: '{}'", workspaceId, cleanQuery);
                return cached.get();
            }
        }

        // 4. Retrieve Authoritative Workspace Documents
        RAGContext ragContext = ragRetrievalService.retrieveAndAssembleContext(effectiveQuery, workspaceId, effectiveTopK, authenticatedUser);

        // 5. Build Final Prompt
        RAGPrompt ragPrompt = ragPromptBuilder.buildPrompt(cleanQuery, ragContext, conversationMemory);

        // 6. Call Gemini
        String generatedAnswer = callGeminiGenerateContent(ragPrompt.fullPrompt());

        // 7. Extract Citations
        List<RAGCitation> citations = new ArrayList<>();
        if (ragContext != null && ragContext.chunks() != null) {
            for (var chunk : ragContext.chunks()) {
                RAGCitation citation = RAGCitation.fromChunk(chunk);
                if (citation != null) {
                    citations.add(citation);
                }
            }
        }

        boolean hasContext = ragContext != null && ragContext.chunks() != null && !ragContext.chunks().isEmpty();

        RAGResponse response = new RAGResponse(
                generatedAnswer,
                cleanQuery,
                workspaceId,
                citations,
                ragContext != null && ragContext.chunks() != null ? ragContext.chunks() : Collections.emptyList(),
                hasContext
        );

        // 8. Store in Semantic Cache
        if (!hasMemoryHistory && semanticCacheService != null) {
            semanticCacheService.store(effectiveQuery, workspaceId, authenticatedUser, response);
        }

        logger.info("Successfully completed RAG generation for workspace id: {} (chunks used: {}, citations: {})",
                workspaceId, ragContext != null && ragContext.chunks() != null ? ragContext.chunks().size() : 0, citations.size());

        return response;
    }

    protected String callGeminiGenerateContent(String promptText) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            logger.warn("Gemini API key not configured for generation. Returning fallback message.");
            return "Gemini API key is not configured. Please set the 'gemini.api.key' property to enable AI response generation.";
        }

        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                generationModel, geminiApiKey
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", promptText)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 2048
                )
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                    if (!textNode.isMissingNode()) {
                        return textNode.asText().trim();
                    }
                }
            }
            logger.warn("Gemini generation API returned unexpected response format.");
            return "Unable to generate answer from the provided documents.";
        } catch (Exception e) {
            logger.error("Gemini generation API call failed: {}", e.getMessage());
            return "An error occurred while communicating with Gemini API: " + e.getMessage();
        }
    }
}
