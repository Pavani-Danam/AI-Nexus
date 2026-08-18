package com.ainexus.service.impl;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.dto.RAGCitation;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.RAGPrompt;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.service.ConversationMemoryService;
import com.ainexus.service.ConversationQueryRewriteService;
import com.ainexus.service.MemoryRetrievalService;
import com.ainexus.service.RAGGenerationService;
import com.ainexus.service.RAGPromptBuilder;
import com.ainexus.service.RAGRetrievalService;
import com.ainexus.service.SemanticCacheService;
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

    @Value("${gemini.model.generation:gemini-1.5-flash}")
    private String generationModel;

    private final RAGRetrievalService ragRetrievalService;
    private final RAGPromptBuilder ragPromptBuilder;
    private final RestTemplate restTemplate = new RestTemplate();

    private SemanticCacheService semanticCacheService;
    private ConversationMemoryService conversationMemoryService;
    private ConversationQueryRewriteService conversationQueryRewriteService;
    private MemoryRetrievalService memoryRetrievalService;

    public RAGGenerationServiceImpl(RAGRetrievalService ragRetrievalService, RAGPromptBuilder ragPromptBuilder) {
        this.ragRetrievalService = ragRetrievalService;
        this.ragPromptBuilder = ragPromptBuilder;
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
    public RAGResponse generateAnswer(String userQuery, Long workspaceId, Integer topK, User user) {
        return generateAnswer(userQuery, workspaceId, topK, null, user);
    }

    @Override
    public RAGResponse generateAnswer(String userQuery, Long workspaceId, Integer topK, Long conversationId, User user) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("User query must not be blank.");
        }
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null.");
        }
        if (user == null) {
            throw new UnauthorizedAccessException("Authenticated user required.");
        }

        int limit = (topK != null && topK > 0) ? topK : 5;
        String cleanQuery = userQuery.trim();
        logger.info("Starting RAG generation for workspace id: {} with query: '{}' (conversationId: {})",
                workspaceId, cleanQuery, conversationId);

        // 1. Fetch Relevance-Filtered Conversational Memory if conversationId is supplied
        ConversationMemory memory = null;
        if (conversationId != null) {
            if (memoryRetrievalService != null) {
                memory = memoryRetrievalService.retrieveRelevantMemory(cleanQuery, conversationId, workspaceId, user);
            } else if (conversationMemoryService != null) {
                memory = conversationMemoryService.getMemory(conversationId, workspaceId, user);
            }
        }

        // 2. Conversation-Aware Query Rewriting for retrieval
        String effectiveRetrievalQuery = cleanQuery;
        if (memory != null && memory.hasHistory() && conversationQueryRewriteService != null) {
            effectiveRetrievalQuery = conversationQueryRewriteService.rewriteToStandaloneQuery(
                    cleanQuery, memory, workspaceId, user
            );
        }

        // 3. Semantic Cache Check (Bypassed if active multi-turn conversational history is present)
        boolean hasMultiTurnMemory = (memory != null && memory.hasHistory());
        if (!hasMultiTurnMemory && semanticCacheService != null) {
            Optional<RAGResponse> cachedResponse = semanticCacheService.lookup(cleanQuery, workspaceId, user);
            if (cachedResponse.isPresent()) {
                logger.info("Returning cached response for workspace id: {} and query: '{}'", workspaceId, cleanQuery);
                return cachedResponse.get();
            }
        }

        // 4. Retrieve Context using the effective retrieval query
        RAGContext ragContext = ragRetrievalService.retrieveAndAssembleContext(
                effectiveRetrievalQuery, workspaceId, limit, user
        );

        // 5. Construct Grounded RAG Prompt with Relevant Memory and Retrieved Chunks
        RAGPrompt ragPrompt = ragPromptBuilder.buildPrompt(cleanQuery, ragContext, memory);

        // 6. Call LLM to synthesize answer
        String generatedAnswer;
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                generatedAnswer = callGeminiGenerateContent(ragPrompt.fullPrompt());
            } catch (Exception e) {
                logger.error("Failed to generate content from Gemini for workspace id: {}: {}", workspaceId, e.getMessage(), e);
                generatedAnswer = "I'm sorry, an error occurred while generating the answer. Please try again.";
            }
        } else {
            logger.warn("Gemini API key not configured. Returning fallback response for workspace id: {}", workspaceId);
            generatedAnswer = "Gemini API key is not configured on the server. Retrieved " +
                    ragContext.chunks().size() + " relevant chunks.";
        }

        // 7. Extract Citations from Chunks
        List<RAGCitation> citations = new ArrayList<>();
        for (var chunk : ragContext.chunks()) {
            citations.add(RAGCitation.fromChunk(chunk));
        }

        boolean hasContext = (ragContext.chunks() != null && !ragContext.chunks().isEmpty());

        RAGResponse ragResponse = new RAGResponse(
                generatedAnswer,
                cleanQuery,
                workspaceId,
                citations,
                ragContext.chunks(),
                hasContext
        );

        // 8. Cache the response (only for single-turn / standalone interactions)
        if (!hasMultiTurnMemory && semanticCacheService != null) {
            semanticCacheService.store(cleanQuery, workspaceId, user, ragResponse);
        }

        logger.info("Successfully completed RAG generation for workspace id: {} (chunks used: {}, citations: {})",
                workspaceId, ragContext.chunks().size(), citations.size());

        return ragResponse;
    }

    protected String callGeminiGenerateContent(String promptText) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + generationModel + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> textPart = Map.of("text", promptText);
        Map<String, Object> contentObj = Map.of("parts", List.of(textPart));
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(contentObj),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 1024
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            List<?> candidates = (List<?>) response.getBody().get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                if (content != null) {
                    List<?> parts = (List<?>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map<?, ?> part = (Map<?, ?>) parts.get(0);
                        return (String) part.get("text");
                    }
                }
            }
        }
        return "No response generated from Gemini.";
    }
}
