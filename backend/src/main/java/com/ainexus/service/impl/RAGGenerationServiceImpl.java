package com.ainexus.service.impl;

import com.ainexus.dto.RAGCitation;
import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.RAGPrompt;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;
import com.ainexus.service.RAGGenerationService;
import com.ainexus.service.RAGPromptBuilder;
import com.ainexus.service.RAGRetrievalService;
import com.ainexus.service.SemanticCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RAGGenerationServiceImpl implements RAGGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(RAGGenerationServiceImpl.class);
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RAGRetrievalService ragRetrievalService;
    private final RAGPromptBuilder ragPromptBuilder;
    private final ObjectMapper objectMapper;
    private SemanticCacheService semanticCacheService;

    @Value("${app.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini.generation-model:gemini-1.5-flash}")
    private String generationModel;

    @Value("${app.ai.gemini.temperature:0.2}")
    private double temperature;

    @Value("${app.ai.gemini.max-output-tokens:2048}")
    private int maxOutputTokens;

    @Value("${app.ai.gemini.timeout-seconds:30}")
    private int timeoutSeconds;

    public RAGGenerationServiceImpl(RAGRetrievalService ragRetrievalService, RAGPromptBuilder ragPromptBuilder) {
        this.ragRetrievalService = ragRetrievalService;
        this.ragPromptBuilder = ragPromptBuilder;
        this.objectMapper = new ObjectMapper();
    }

    @Autowired(required = false)
    public void setSemanticCacheService(SemanticCacheService semanticCacheService) {
        this.semanticCacheService = semanticCacheService;
    }

    @Override
    public RAGResponse generateAnswer(String query, Long workspaceId, Integer topK, User authenticatedUser) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query must not be null or blank.");
        }
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null.");
        }

        String cleanQuery = query.trim();

        // 1. Semantic Cache Lookup
        if (semanticCacheService != null) {
            Optional<RAGResponse> cachedResponse = semanticCacheService.lookup(cleanQuery, workspaceId, authenticatedUser);
            if (cachedResponse.isPresent()) {
                logger.info("Returning cached response for workspace id: {} and query: '{}'", workspaceId, cleanQuery);
                return cachedResponse.get();
            }
        }

        logger.info("Starting RAG generation for workspace id: {} with query: '{}'", workspaceId, cleanQuery);

        // 2. Retrieve authorized context
        RAGContext ragContext = ragRetrievalService.retrieveAndAssembleContext(cleanQuery, workspaceId, topK, authenticatedUser);

        // 3. Build structured RAG prompt
        RAGPrompt ragPrompt = ragPromptBuilder.buildPrompt(cleanQuery, ragContext);

        // 4. Call Gemini model
        String answer = callGeminiGenerateContent(ragPrompt.fullPrompt());

        // 5. Build authoritative citations from retrieved chunks (deduplicated by documentId + chunkIndex)
        List<RAGCitation> citations = buildAuthoritativeCitations(ragContext.chunks());

        logger.info("Successfully completed RAG generation for workspace id: {} (chunks used: {}, citations: {})",
                workspaceId, ragContext.chunks().size(), citations.size());

        RAGResponse response = new RAGResponse(
                answer,
                cleanQuery,
                workspaceId,
                citations,
                ragContext.chunks(),
                ragPrompt.hasContext()
        );

        // 6. Store successful response in semantic cache
        if (semanticCacheService != null) {
            semanticCacheService.store(cleanQuery, workspaceId, authenticatedUser, response);
        }

        return response;
    }

    private List<RAGCitation> buildAuthoritativeCitations(List<RAGChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> seenKeys = new LinkedHashSet<>();
        List<RAGCitation> result = new ArrayList<>();

        for (RAGChunk chunk : chunks) {
            if (chunk != null) {
                String key = chunk.documentId() + ":" + chunk.chunkIndex();
                if (seenKeys.add(key)) {
                    result.add(RAGCitation.fromChunk(chunk));
                }
            }
        }
        return result;
    }

    protected String callGeminiGenerateContent(String promptText) {
        validateApiKey();

        try {
            String endpoint = GEMINI_API_BASE_URL + generationModel + ":generateContent?key=" + geminiApiKey;
            URI uri = URI.create(endpoint);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(timeoutSeconds * 1000);
            conn.setReadTimeout(timeoutSeconds * 1000);
            conn.setDoOutput(true);

            // Construct Gemini generateContent payload
            Map<String, Object> textPart = Map.of("text", promptText);
            Map<String, Object> contentMap = Map.of("parts", List.of(textPart));

            Map<String, Object> generationConfig = Map.of(
                    "temperature", temperature,
                    "maxOutputTokens", maxOutputTokens
            );

            Map<String, Object> requestPayload = Map.of(
                    "contents", List.of(contentMap),
                    "generationConfig", generationConfig
            );

            byte[] bodyBytes = objectMapper.writeValueAsBytes(requestPayload);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorDetails = readStream(conn.getErrorStream());
                logger.error("Gemini Generation API error (HTTP {}): {}", responseCode, sanitizeError(errorDetails));
                throw new RuntimeException("Gemini generation provider returned error (HTTP " + responseCode + ")");
            }

            try (InputStream is = conn.getInputStream()) {
                Map<String, Object> responseMap = objectMapper.readValue(is, new TypeReference<>() {});
                return extractTextFromGeminiResponse(responseMap);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to generate RAG content with Gemini: {}", e.getMessage());
            throw new RuntimeException("Failed to generate answer with Gemini provider: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map<String, Object> responseMap) {
        if (responseMap == null || !responseMap.containsKey("candidates")) {
            throw new RuntimeException("Malformed response structure from Gemini: missing 'candidates'");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("Gemini returned empty candidates list");
        }

        Map<String, Object> firstCandidate = candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
        if (content == null || !content.containsKey("parts")) {
            throw new RuntimeException("Malformed response candidate from Gemini: missing 'content.parts'");
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new RuntimeException("Gemini returned empty parts list in candidate content");
        }

        StringBuilder fullText = new StringBuilder();
        for (Map<String, Object> part : parts) {
            if (part != null && part.containsKey("text")) {
                fullText.append(part.get("text"));
            }
        }

        String result = fullText.toString().trim();
        if (result.isEmpty()) {
            throw new RuntimeException("Gemini returned empty text in response parts");
        }

        return result;
    }

    private void validateApiKey() {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            throw new RuntimeException("Gemini API key is not configured. Set GEMINI_API_KEY environment variable.");
        }
    }

    private String readStream(InputStream stream) {
        if (stream == null) return "No error body";
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Failed to read error body";
        }
    }

    private String sanitizeError(String raw) {
        if (raw == null) return "";
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            return raw.replace(geminiApiKey, "******");
        }
        return raw;
    }
}
