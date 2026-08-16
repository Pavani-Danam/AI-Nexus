package com.ainexus.service.impl;

import com.ainexus.exception.EmbeddingException;
import com.ainexus.service.EmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.ai.embedding.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiEmbeddingServiceImpl implements EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiEmbeddingServiceImpl.class);
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final ObjectMapper objectMapper;

    @Value("${app.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini.embedding-model:text-embedding-004}")
    private String embeddingModel;

    @Value("${app.ai.embedding.timeout-seconds:15}")
    private int timeoutSeconds;

    public GeminiEmbeddingServiceImpl() {
        this.objectMapper = new ObjectMapper();
    }

    public GeminiEmbeddingServiceImpl(String apiKey, String model, int timeoutSeconds) {
        this.objectMapper = new ObjectMapper();
        this.geminiApiKey = apiKey;
        this.embeddingModel = model != null ? model : "text-embedding-004";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 15;
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        validateTextInput(text);
        validateApiKey();

        try {
            String endpoint = GEMINI_API_BASE_URL + embeddingModel + ":embedContent?key=" + geminiApiKey;
            URI uri = URI.create(endpoint);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(timeoutSeconds * 1000);
            conn.setReadTimeout(timeoutSeconds * 1000);
            conn.setDoOutput(true);

            Map<String, Object> textPart = Map.of("text", text);
            Map<String, Object> contentMap = Map.of("parts", List.of(textPart));
            Map<String, Object> requestPayload = Map.of(
                    "model", "models/" + embeddingModel,
                    "content", contentMap
            );

            byte[] bodyBytes = objectMapper.writeValueAsBytes(requestPayload);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorDetails = readStream(conn.getErrorStream());
                logger.error("Gemini API error (HTTP {}): {}", responseCode, sanitizeError(errorDetails));
                throw new EmbeddingException("Gemini embedding provider returned error (HTTP " + responseCode + ")");
            }

            try (InputStream is = conn.getInputStream()) {
                Map<String, Object> responseMap = objectMapper.readValue(is, new TypeReference<>() {});
                Map<String, Object> embeddingNode = (Map<String, Object>) responseMap.get("embedding");
                if (embeddingNode == null || !embeddingNode.containsKey("values")) {
                    throw new EmbeddingException("Malformed embedding response structure from Gemini");
                }

                List<Number> rawValues = (List<Number>) embeddingNode.get("values");
                List<Float> vector = new ArrayList<>(rawValues.size());
                for (Number val : rawValues) {
                    vector.add(val.floatValue());
                }
                return vector;
            }
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to generate embedding: {}", e.getMessage());
            throw new EmbeddingException("Failed to generate embedding with Gemini provider: " + e.getMessage(), e);
        }
    }

    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<Float>> embeddings = new ArrayList<>(texts.size());
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        return embeddings;
    }

    private void validateTextInput(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text to embed cannot be null, empty, or whitespace-only");
        }
    }

    private void validateApiKey() {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            throw new EmbeddingException("Gemini API key is not configured. Set GEMINI_API_KEY environment variable.");
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
