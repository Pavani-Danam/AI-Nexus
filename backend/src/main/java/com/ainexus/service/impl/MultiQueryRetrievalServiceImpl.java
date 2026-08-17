package com.ainexus.service.impl;

import com.ainexus.dto.SearchResponse;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;
import com.ainexus.service.MultiQueryRetrievalService;
import com.ainexus.service.SemanticSearchService;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MultiQueryRetrievalServiceImpl implements MultiQueryRetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(MultiQueryRetrievalServiceImpl.class);
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final SemanticSearchService semanticSearchService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini.generation-model:gemini-1.5-flash}")
    private String generationModel;

    @Value("${app.rag.multi-query.enabled:true}")
    private boolean multiQueryEnabled;

    @Value("${app.rag.multi-query.max-variations:3}")
    private int maxVariations;

    @Value("${app.ai.gemini.timeout-seconds:15}")
    private int timeoutSeconds;

    public MultiQueryRetrievalServiceImpl(SemanticSearchService semanticSearchService) {
        this.semanticSearchService = Objects.requireNonNull(semanticSearchService, "SemanticSearchService must not be null");
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<SearchResultItem> retrieveMultiQueryResults(String primaryRetrievalQuery,
                                                            Long workspaceId,
                                                            Integer topK,
                                                            User authenticatedUser) {
        if (primaryRetrievalQuery == null || primaryRetrievalQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Primary retrieval query must not be null or blank.");
        }
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID must not be null.");
        }

        int effectiveTopK = (topK != null && topK > 0) ? topK : 5;

        // 1. Generate query variations (always includes the primary query)
        List<String> queryVariations = generateQueryVariations(primaryRetrievalQuery.trim());
        logger.info("Executing multi-query retrieval for workspace id: {} with {} queries: {}",
                workspaceId, queryVariations.size(), queryVariations);

        // 2. Execute retrieval for each query variation and collect chunks
        Map<String, SearchResultItem> deduplicatedChunks = new LinkedHashMap<>();

        for (String query : queryVariations) {
            try {
                SearchResponse response = semanticSearchService.search(query, workspaceId, effectiveTopK, authenticatedUser);
                if (response != null && response.results() != null) {
                    for (SearchResultItem item : response.results()) {
                        if (item != null) {
                            String key = buildChunkKey(item);
                            deduplicatedChunks.compute(key, (k, existing) -> {
                                if (existing == null) {
                                    return item;
                                }
                                // Retain the maximum similarity score when chunk is retrieved across multiple queries
                                double maxScore = Math.max(
                                        existing.score() != null ? existing.score() : 0.0,
                                        item.score() != null ? item.score() : 0.0
                                );
                                return new SearchResultItem(
                                        item.documentId(),
                                        item.filename(),
                                        item.chunkIndex(),
                                        maxScore,
                                        item.content(),
                                        item.characterCount(),
                                        item.fileType(),
                                        item.vectorId()
                                );
                            });
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Retrieval failed for query variation '{}' in workspace id {}: {}", query, workspaceId, e.getMessage());
            }
        }

        // 3. Sort deterministically by highest score descending
        List<SearchResultItem> sortedResults = deduplicatedChunks.values().stream()
                .sorted((a, b) -> {
                    double scoreA = a.score() != null ? a.score() : 0.0;
                    double scoreB = b.score() != null ? b.score() : 0.0;
                    return Double.compare(scoreB, scoreA);
                })
                .collect(Collectors.toList());

        // Bounded result limit (topK * 2 max to ensure ample diverse candidates for ContextManagementService)
        int maxResults = Math.max(effectiveTopK * 2, 10);
        if (sortedResults.size() > maxResults) {
            sortedResults = sortedResults.subList(0, maxResults);
        }

        logger.info("Multi-query retrieval merged {} unique chunks for workspace id: {}", sortedResults.size(), workspaceId);
        return sortedResults;
    }

    private String buildChunkKey(SearchResultItem item) {
        if (item.documentId() != null && item.chunkIndex() != null) {
            return item.documentId() + ":" + item.chunkIndex();
        }
        if (item.vectorId() != null && !item.vectorId().isEmpty()) {
            return item.vectorId();
        }
        return UUID.randomUUID().toString();
    }

    private List<String> generateQueryVariations(String primaryQuery) {
        Set<String> queries = new LinkedHashSet<>();
        queries.add(primaryQuery);

        if (!multiQueryEnabled || maxVariations <= 1 || geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            return new ArrayList<>(queries);
        }

        try {
            List<String> variations = callGeminiForVariations(primaryQuery, maxVariations - 1);
            for (String v : variations) {
                if (v != null && !v.trim().isEmpty() && queries.size() < maxVariations) {
                    queries.add(v.trim());
                }
            }
        } catch (Exception e) {
            logger.warn("Multi-query variation generation failed for '{}': {}. Falling back to single query.", primaryQuery, e.getMessage());
        }

        return new ArrayList<>(queries);
    }

    protected List<String> callGeminiForVariations(String query, int count) throws Exception {
        String endpoint = GEMINI_API_BASE_URL + generationModel + ":generateContent?key=" + geminiApiKey;
        URI uri = URI.create(endpoint);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(timeoutSeconds * 1000);
        conn.setReadTimeout(timeoutSeconds * 1000);
        conn.setDoOutput(true);

        String systemInstruction = """
                You are a search query expansion assistant.
                Generate %d distinct alternative search phrases for document vector retrieval based on the user's query.
                Rules:
                - Preserve the user's exact topic and intent.
                - Do NOT answer the query.
                - Do NOT add numbered prefixes, bullets, explanations, or quotes.
                - Output each alternative search phrase on a new line.
                """.formatted(count);

        String prompt = systemInstruction + "\n\nUser Query: " + query + "\nAlternative Queries:\n";

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contentMap = Map.of("parts", List.of(textPart));
        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.3,
                "maxOutputTokens", 128
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
            throw new RuntimeException("Gemini multi-query HTTP error " + responseCode);
        }

        try (InputStream is = conn.getInputStream()) {
            Map<String, Object> responseMap = objectMapper.readValue(is, new TypeReference<>() {});
            return parseVariations(extractTextFromGeminiResponse(responseMap));
        }
    }

    private List<String> parseVariations(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<>();
        String[] lines = rawText.split("\n");
        for (String line : lines) {
            String cleaned = line.replaceAll("^[0-9]+[.\\-\\)]\\s*", "")
                    .replaceAll("^[\\-*]\\s*", "")
                    .replaceAll("[\"']", "")
                    .trim();
            if (!cleaned.isEmpty()) {
                list.add(cleaned);
            }
        }
        return list;
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
