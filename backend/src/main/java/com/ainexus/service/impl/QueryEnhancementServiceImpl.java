package com.ainexus.service.impl;

import com.ainexus.dto.EnhancedQuery;
import com.ainexus.service.QueryEnhancementService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class QueryEnhancementServiceImpl implements QueryEnhancementService {

    private static final Logger logger = LoggerFactory.getLogger(QueryEnhancementServiceImpl.class);
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");

    private final ObjectMapper objectMapper;

    @Value("${app.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini.generation-model:gemini-1.5-flash}")
    private String generationModel;

    @Value("${app.rag.query-enhancement.enabled:true}")
    private boolean enhancementEnabled;

    @Value("${app.rag.max-rewritten-query-length:300}")
    private int maxRewrittenQueryLength;

    @Value("${app.ai.gemini.timeout-seconds:15}")
    private int timeoutSeconds;

    public QueryEnhancementServiceImpl() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public EnhancedQuery enhanceQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Query must not be null or blank.");
        }

        String cleaned = originalQuery.trim();

        // 1. If enhancement is disabled or query is already clear and descriptive, preserve as-is
        if (!enhancementEnabled || isAlreadyClearAndSpecific(cleaned)) {
            logger.debug("Query '{}' is already specific or enhancement is disabled. Keeping original.", cleaned);
            return EnhancedQuery.unchanged(cleaned);
        }

        // 2. If API key is missing, fall back safely
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            logger.warn("Gemini API key not configured for query enhancement. Falling back to original query: '{}'", cleaned);
            return EnhancedQuery.unchanged(cleaned);
        }

        try {
            String rewritten = callGeminiQueryRewriter(cleaned);
            if (rewritten == null || rewritten.trim().isEmpty()) {
                logger.warn("Query rewriting produced empty result for '{}'. Falling back to original.", cleaned);
                return EnhancedQuery.unchanged(cleaned);
            }

            String sanitized = sanitizeRewrittenQuery(rewritten);
            if (sanitized.length() > maxRewrittenQueryLength) {
                sanitized = sanitized.substring(0, maxRewrittenQueryLength).trim();
            }

            logger.info("Enhanced query '{}' -> '{}'", cleaned, sanitized);
            return EnhancedQuery.of(cleaned, sanitized);
        } catch (Exception e) {
            logger.warn("Query enhancement failed for '{}': {}. Falling back to original query.", cleaned, e.getMessage());
            return EnhancedQuery.unchanged(cleaned);
        }
    }

    /**
     * Determines whether a query is already specific enough to not require LLM rewriting.
     */
    private boolean isAlreadyClearAndSpecific(String query) {
        String[] words = query.split("\\s+");
        if (words.length >= 6) {
            return true;
        }

        String lower = query.toLowerCase();
        return (lower.startsWith("what is the ") || lower.startsWith("how do i ") || lower.startsWith("where can i find ") || lower.startsWith("explain how "))
                && words.length >= 4;
    }

    private String sanitizeRewrittenQuery(String raw) {
        String cleaned = raw.replaceAll("[\"'\n\r\t]", " ").trim();
        if (cleaned.toLowerCase().startsWith("improved query:")) {
            cleaned = cleaned.substring("improved query:".length()).trim();
        } else if (cleaned.toLowerCase().startsWith("query:")) {
            cleaned = cleaned.substring("query:".length()).trim();
        } else if (cleaned.toLowerCase().startsWith("search query:")) {
            cleaned = cleaned.substring("search query:".length()).trim();
        }
        return MULTIPLE_SPACES_PATTERN.matcher(cleaned).replaceAll(" ").trim();
    }

    protected String callGeminiQueryRewriter(String userQuery) throws Exception {
        String endpoint = GEMINI_API_BASE_URL + generationModel + ":generateContent?key=" + geminiApiKey;
        URI uri = URI.create(endpoint);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(timeoutSeconds * 1000);
        conn.setReadTimeout(timeoutSeconds * 1000);
        conn.setDoOutput(true);

        String systemInstruction = """
                You are a search query optimizer for a document retrieval system.
                Transform the user's search query into a clear, precise, and concise retrieval query.
                Rules:
                - Preserve the user's exact search intent.
                - Do NOT answer the query.
                - Do NOT invent document names, companies, or facts.
                - Do NOT add filler words or explanations.
                - Return ONLY the single improved search query sentence.
                """;

        String prompt = systemInstruction + "\n\nUser Query: " + userQuery + "\nImproved Search Query:";

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contentMap = Map.of("parts", List.of(textPart));
        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.0,
                "maxOutputTokens", 64
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
            throw new RuntimeException("Gemini rewriter HTTP error " + responseCode);
        }

        try (InputStream is = conn.getInputStream()) {
            Map<String, Object> responseMap = objectMapper.readValue(is, new TypeReference<>() {});
            return extractTextFromGeminiResponse(responseMap);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map<String, Object> responseMap) {
        if (responseMap == null || !responseMap.containsKey("candidates")) {
            return null;
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Map<String, Object> firstCandidate = candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
        if (content == null || !content.containsKey("parts")) {
            return null;
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        StringBuilder fullText = new StringBuilder();
        for (Map<String, Object> part : parts) {
            if (part != null && part.containsKey("text")) {
                fullText.append(part.get("text"));
            }
        }
        return fullText.toString().trim();
    }
}
