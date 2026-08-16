package com.ainexus.service;

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
import java.security.MessageDigest;
import java.util.*;

@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int DEFAULT_VECTOR_DIMENSION = 384;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${app.ai.openai.embedding-model:text-embedding-3-small}")
    private String openAiEmbeddingModel;

    @Value("${app.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${app.ai.ollama.embedding-model:nomic-embed-text}")
    private String ollamaEmbeddingModel;

    @Value("${app.ai.embedding.provider:fallback}")
    private String embeddingProvider;

    public List<Double> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            if ("openai".equalsIgnoreCase(embeddingProvider) && openAiApiKey != null && !openAiApiKey.isBlank()) {
                return generateOpenAiEmbedding(text);
            } else if ("gemini".equalsIgnoreCase(embeddingProvider) && geminiApiKey != null && !geminiApiKey.isBlank()) {
                return generateGeminiEmbedding(text);
            } else if ("ollama".equalsIgnoreCase(embeddingProvider)) {
                return generateOllamaEmbedding(text);
            }
        } catch (Exception e) {
            logger.warn("Provider embedding generation failed ({}). Falling back to deterministic local embedding.", e.getMessage());
        }

        return generateDeterministicEmbedding(text, DEFAULT_VECTOR_DIMENSION);
    }

    public String serializeEmbedding(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            logger.error("Error serializing vector: {}", e.getMessage());
            return "[]";
        }
    }

    public List<Double> deserializeEmbedding(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            logger.error("Error deserializing vector: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public double calculateCosineSimilarity(List<Double> vecA, List<Double> vecB) {
        if (vecA == null || vecB == null || vecA.isEmpty() || vecB.isEmpty() || vecA.size() != vecB.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.size(); i++) {
            double a = vecA.get(i);
            double b = vecB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<Double> generateOpenAiEmbedding(String text) throws Exception {
        URI uri = URI.create("https://api.openai.com/v1/embeddings");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + openAiApiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        Map<String, Object> payload = Map.of(
                "model", openAiEmbeddingModel,
                "input", text
        );

        byte[] bodyBytes = objectMapper.writeValueAsBytes(payload);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("OpenAI API error: HTTP " + conn.getResponseCode());
        }

        try (InputStream is = conn.getInputStream()) {
            Map<String, Object> resp = objectMapper.readValue(is, new TypeReference<>() {});
            List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");
            if (data != null && !data.isEmpty()) {
                return (List<Double>) data.get(0).get("embedding");
            }
        }
        throw new RuntimeException("Empty response from OpenAI embeddings API");
    }

    private List<Double> generateGeminiEmbedding(String text) throws Exception {
        URI uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=" + geminiApiKey);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        Map<String, Object> contentMap = Map.of("parts", List.of(Map.of("text", text)));
        Map<String, Object> payload = Map.of(
                "model", "models/text-embedding-004",
                "content", contentMap
        );

        byte[] bodyBytes = objectMapper.writeValueAsBytes(payload);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Gemini API error: HTTP " + conn.getResponseCode());
        }

        try (InputStream is = conn.getInputStream()) {
            Map<String, Object> resp = objectMapper.readValue(is, new TypeReference<>() {});
            Map<String, Object> embeddingMap = (Map<String, Object>) resp.get("embedding");
            if (embeddingMap != null && embeddingMap.containsKey("values")) {
                return (List<Double>) embeddingMap.get("values");
            }
        }
        throw new RuntimeException("Empty response from Gemini embeddings API");
    }

    private List<Double> generateOllamaEmbedding(String text) throws Exception {
        URI uri = URI.create(ollamaBaseUrl + "/api/embeddings");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        Map<String, Object> payload = Map.of(
                "model", ollamaEmbeddingModel,
                "prompt", text
        );

        byte[] bodyBytes = objectMapper.writeValueAsBytes(payload);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Ollama API error: HTTP " + conn.getResponseCode());
        }

        try (InputStream is = conn.getInputStream()) {
            Map<String, Object> resp = objectMapper.readValue(is, new TypeReference<>() {});
            if (resp.containsKey("embedding")) {
                return (List<Double>) resp.get("embedding");
            }
        }
        throw new RuntimeException("Empty response from Ollama embeddings API");
    }

    public List<Double> generateDeterministicEmbedding(String text, int dimensions) {
        List<Double> vector = new ArrayList<>(dimensions);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.toLowerCase(Locale.ROOT).trim().getBytes(StandardCharsets.UTF_8));
            Random random = new Random(new java.math.BigInteger(1, hash).longValue());

            double norm = 0.0;
            for (int i = 0; i < dimensions; i++) {
                double val = (random.nextDouble() * 2.0) - 1.0;
                vector.add(val);
                norm += val * val;
            }

            norm = Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < dimensions; i++) {
                    vector.set(i, vector.get(i) / norm);
                }
            }
        } catch (Exception e) {
            for (int i = 0; i < dimensions; i++) {
                vector.add(0.0);
            }
        }
        return vector;
    }
}
