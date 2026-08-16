package com.ainexus.service;

import com.ainexus.exception.EmbeddingException;
import com.ainexus.service.impl.GeminiEmbeddingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeminiEmbeddingServiceImplTest {

    @Test
    @DisplayName("Should throw IllegalArgumentException when text is null")
    void testNullTextRejection() {
        GeminiEmbeddingServiceImpl service = new GeminiEmbeddingServiceImpl("test-api-key", "text-embedding-004", 15);
        assertThrows(IllegalArgumentException.class, () -> service.generateEmbedding(null));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when text is empty")
    void testEmptyTextRejection() {
        GeminiEmbeddingServiceImpl service = new GeminiEmbeddingServiceImpl("test-api-key", "text-embedding-004", 15);
        assertThrows(IllegalArgumentException.class, () -> service.generateEmbedding(""));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when text is whitespace only")
    void testWhitespaceOnlyTextRejection() {
        GeminiEmbeddingServiceImpl service = new GeminiEmbeddingServiceImpl("test-api-key", "text-embedding-004", 15);
        assertThrows(IllegalArgumentException.class, () -> service.generateEmbedding("   \n\t  "));
    }

    @Test
    @DisplayName("Should throw EmbeddingException when API key is missing or blank")
    void testMissingApiKeyRejection() {
        GeminiEmbeddingServiceImpl service = new GeminiEmbeddingServiceImpl("", "text-embedding-004", 15);
        EmbeddingException ex = assertThrows(EmbeddingException.class, () -> service.generateEmbedding("valid text to embed"));
        assertTrue(ex.getMessage().contains("Gemini API key is not configured"));
    }

    @Test
    @DisplayName("Should return empty list when batch list is empty or null")
    void testEmptyBatchEmbeddings() {
        GeminiEmbeddingServiceImpl service = new GeminiEmbeddingServiceImpl("test-api-key", "text-embedding-004", 15);
        List<List<Float>> emptyResult = service.generateEmbeddings(List.of());
        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());

        List<List<Float>> nullResult = service.generateEmbeddings(null);
        assertNotNull(nullResult);
        assertTrue(nullResult.isEmpty());
    }
}
