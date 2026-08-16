package com.ainexus.service.impl;

import com.ainexus.exception.VectorStoreException;
import com.ainexus.model.vector.VectorQueryResult;
import com.ainexus.model.vector.VectorRecord;
import com.ainexus.service.VectorStoreService;
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

@Service
public class PineconeVectorStoreServiceImpl implements VectorStoreService {

    private static final Logger logger = LoggerFactory.getLogger(PineconeVectorStoreServiceImpl.class);

    private final ObjectMapper objectMapper;

    @Value("${app.ai.pinecone.api-key:}")
    private String apiKey;

    @Value("${app.ai.pinecone.host:}")
    private String host;

    @Value("${app.ai.pinecone.dimension:768}")
    private int expectedDimension;

    @Value("${app.ai.pinecone.default-top-k:5}")
    private int defaultTopK;

    @Value("${app.ai.pinecone.timeout-seconds:15}")
    private int timeoutSeconds;

    public PineconeVectorStoreServiceImpl() {
        this.objectMapper = new ObjectMapper();
    }

    public PineconeVectorStoreServiceImpl(String apiKey, String host, int dimension, int defaultTopK, int timeoutSeconds) {
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.host = host;
        this.expectedDimension = dimension > 0 ? dimension : 768;
        this.defaultTopK = defaultTopK > 0 ? defaultTopK : 5;
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 15;
    }

    @Override
    public void upsert(Long workspaceId, List<VectorRecord> records) {
        validateWorkspaceId(workspaceId);
        if (records == null || records.isEmpty()) {
            return;
        }

        validateConfig();

        for (VectorRecord record : records) {
            validateVectorDimension(record.values());
        }

        String namespace = getNamespace(workspaceId);
        List<Map<String, Object>> pineconeVectors = new ArrayList<>(records.size());

        for (VectorRecord record : records) {
            Map<String, Object> metadata = new HashMap<>(record.metadata());
            metadata.put("workspaceId", workspaceId);

            Map<String, Object> vectorMap = new HashMap<>();
            vectorMap.put("id", record.id());
            vectorMap.put("values", record.values());
            vectorMap.put("metadata", metadata);
            pineconeVectors.add(vectorMap);
        }

        Map<String, Object> payload = Map.of(
                "vectors", pineconeVectors,
                "namespace", namespace
        );

        sendRequest("/vectors/upsert", "POST", payload);
    }

    @Override
    public List<VectorQueryResult> query(Long workspaceId, List<Float> queryVector, int topK) {
        validateWorkspaceId(workspaceId);
        if (queryVector == null || queryVector.isEmpty()) {
            throw new IllegalArgumentException("Query vector cannot be null or empty");
        }

        validateVectorDimension(queryVector);
        validateConfig();

        int effectiveTopK = topK > 0 ? topK : defaultTopK;
        String namespace = getNamespace(workspaceId);

        Map<String, Object> payload = Map.of(
                "vector", queryVector,
                "topK", effectiveTopK,
                "includeMetadata", true,
                "includeValues", false,
                "namespace", namespace
        );

        Map<String, Object> response = sendRequest("/query", "POST", payload);
        List<Map<String, Object>> matches = (List<Map<String, Object>>) response.get("matches");
        if (matches == null || matches.isEmpty()) {
            return Collections.emptyList();
        }

        List<VectorQueryResult> results = new ArrayList<>(matches.size());
        for (Map<String, Object> match : matches) {
            String id = (String) match.get("id");
            Number scoreVal = (Number) match.get("score");
            Double score = scoreVal != null ? scoreVal.doubleValue() : 0.0;
            Map<String, Object> metadata = (Map<String, Object>) match.get("metadata");

            results.add(new VectorQueryResult(id, score, metadata != null ? metadata : Collections.emptyMap()));
        }

        return results;
    }

    @Override
    public void deleteByDocumentId(Long workspaceId, Long documentId) {
        validateWorkspaceId(workspaceId);
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }

        validateConfig();

        String namespace = getNamespace(workspaceId);
        Map<String, Object> filter = Map.of("documentId", documentId);
        Map<String, Object> payload = Map.of(
                "filter", filter,
                "namespace", namespace
        );

        sendRequest("/vectors/delete", "POST", payload);
    }

    private String getNamespace(Long workspaceId) {
        return "ws-" + workspaceId;
    }

    private void validateWorkspaceId(Long workspaceId) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("Invalid workspace ID: must be a positive non-null ID");
        }
    }

    private void validateVectorDimension(List<Float> values) {
        if (values == null || values.size() != expectedDimension) {
            throw new VectorStoreException("Vector dimension mismatch: expected " + expectedDimension +
                    " but received " + (values != null ? values.size() : 0));
        }
    }

    private void validateConfig() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new VectorStoreException("Pinecone API key is not configured. Set PINECONE_API_KEY environment variable.");
        }
        if (host == null || host.trim().isEmpty()) {
            throw new VectorStoreException("Pinecone host is not configured. Set PINECONE_HOST environment variable.");
        }
    }

    private Map<String, Object> sendRequest(String path, String method, Map<String, Object> payload) {
        try {
            String cleanHost = host.trim();
            if (!cleanHost.startsWith("http://") && !cleanHost.startsWith("https://")) {
                cleanHost = "https://" + cleanHost;
            }
            if (cleanHost.endsWith("/")) {
                cleanHost = cleanHost.substring(0, cleanHost.length() - 1);
            }

            URI uri = URI.create(cleanHost + path);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Api-Key", apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(timeoutSeconds * 1000);
            conn.setReadTimeout(timeoutSeconds * 1000);
            conn.setDoOutput(true);

            byte[] bodyBytes = objectMapper.writeValueAsBytes(payload);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                String errorBody = readStream(conn.getErrorStream());
                logger.error("Pinecone API error (HTTP {}): {}", responseCode, sanitize(errorBody));
                throw new VectorStoreException("Pinecone vector store error (HTTP " + responseCode + ")");
            }

            try (InputStream is = conn.getInputStream()) {
                return objectMapper.readValue(is, new TypeReference<>() {});
            }
        } catch (VectorStoreException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed communication with Pinecone: {}", e.getMessage());
            throw new VectorStoreException("Failed communication with Pinecone vector database: " + e.getMessage(), e);
        }
    }

    private String readStream(InputStream stream) {
        if (stream == null) return "No response body";
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Unable to read error stream";
        }
    }

    private String sanitize(String raw) {
        if (raw == null) return "";
        if (apiKey != null && !apiKey.isEmpty()) {
            return raw.replace(apiKey, "******");
        }
        return raw;
    }
}
