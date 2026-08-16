package com.ainexus.service;

import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.RAGPrompt;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;
import com.ainexus.service.impl.RAGGenerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RAGGenerationServiceImplTest {

    @Mock
    private RAGRetrievalService ragRetrievalService;

    @Mock
    private RAGPromptBuilder ragPromptBuilder;

    private TestableRAGGenerationService ragGenerationService;
    private User testUser;

    static class TestableRAGGenerationService extends RAGGenerationServiceImpl {
        private String mockResponseText = "This is a grounded answer from Gemini.";
        private RuntimeException errorToThrow = null;
        private String lastPromptReceived = null;

        public TestableRAGGenerationService(RAGRetrievalService retrievalService, RAGPromptBuilder promptBuilder) {
            super(retrievalService, promptBuilder);
        }

        public void setMockResponseText(String text) {
            this.mockResponseText = text;
        }

        public void setErrorToThrow(RuntimeException error) {
            this.errorToThrow = error;
        }

        public String getLastPromptReceived() {
            return lastPromptReceived;
        }

        @Override
        protected String callGeminiGenerateContent(String promptText) {
            this.lastPromptReceived = promptText;
            if (errorToThrow != null) {
                throw errorToThrow;
            }
            return mockResponseText;
        }
    }

    @BeforeEach
    void setUp() {
        ragGenerationService = new TestableRAGGenerationService(ragRetrievalService, ragPromptBuilder);
        ReflectionTestUtils.setField(ragGenerationService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(ragGenerationService, "generationModel", "gemini-1.5-flash");
        ReflectionTestUtils.setField(ragGenerationService, "temperature", 0.2);
        ReflectionTestUtils.setField(ragGenerationService, "maxOutputTokens", 2048);
        ReflectionTestUtils.setField(ragGenerationService, "timeoutSeconds", 30);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: Valid RAG context + query returns grounded answer with authoritative citations")
    void testSuccessfulRAGGenerationWithCitations() {
        RAGChunk chunk = new RAGChunk(10L, "architecture.pdf", 0, 0.91, "Microservices guidelines.", 25);
        RAGContext ragContext = new RAGContext("architecture", 1L, List.of(chunk), "Microservices guidelines.", 25);
        RAGPrompt ragPrompt = new RAGPrompt("System rules", "Context text", "architecture", "Full structured prompt", true);

        when(ragRetrievalService.retrieveAndAssembleContext("architecture", 1L, 5, testUser)).thenReturn(ragContext);
        when(ragPromptBuilder.buildPrompt("architecture", ragContext)).thenReturn(ragPrompt);

        ragGenerationService.setMockResponseText("Microservices architecture is recommended for modularity.");

        RAGResponse response = ragGenerationService.generateAnswer("architecture", 1L, 5, testUser);

        assertNotNull(response);
        assertEquals("Microservices architecture is recommended for modularity.", response.answer());
        assertEquals("architecture", response.query());
        assertEquals(1L, response.workspaceId());
        assertTrue(response.hasContext());
        assertEquals(1, response.citations().size());
        assertEquals(10L, response.citations().get(0).documentId());
        assertEquals("architecture.pdf", response.citations().get(0).filename());
        assertEquals(0, response.citations().get(0).chunkIndex());
        assertEquals(0.91, response.citations().get(0).similarityScore());
        assertEquals("doc-10-chunk-0", response.citations().get(0).sourceId());
        assertEquals("Full structured prompt", ragGenerationService.getLastPromptReceived());
    }

    @Test
    @DisplayName("TEST 2: Duplicate retrieved chunks are deduplicated by documentId and chunkIndex")
    void testDuplicateChunkDeduplication() {
        RAGChunk c1 = new RAGChunk(10L, "policy.pdf", 0, 0.95, "Leave policy text", 17);
        RAGChunk c2 = new RAGChunk(10L, "policy.pdf", 0, 0.95, "Leave policy text duplicate", 27);
        RAGChunk c3 = new RAGChunk(10L, "policy.pdf", 1, 0.88, "Second chunk text", 17);

        RAGContext ragContext = new RAGContext("leave", 1L, List.of(c1, c2, c3), "All text", 61);
        RAGPrompt ragPrompt = new RAGPrompt("System", "Context", "leave", "Prompt", true);

        when(ragRetrievalService.retrieveAndAssembleContext("leave", 1L, 5, testUser)).thenReturn(ragContext);
        when(ragPromptBuilder.buildPrompt("leave", ragContext)).thenReturn(ragPrompt);

        ragGenerationService.setMockResponseText("20 days annual leave.");

        RAGResponse response = ragGenerationService.generateAnswer("leave", 1L, 5, testUser);

        assertNotNull(response);
        assertEquals(2, response.citations().size());
        assertEquals("doc-10-chunk-0", response.citations().get(0).sourceId());
        assertEquals("doc-10-chunk-1", response.citations().get(1).sourceId());
    }

    @Test
    @DisplayName("TEST 3: Empty context returns empty citations list without fake citations")
    void testEmptyContextGenerationReturnsEmptyCitations() {
        RAGContext emptyContext = RAGContext.empty("unmatched query", 1L);
        RAGPrompt emptyPrompt = new RAGPrompt("System rules", "[NO RELEVANT DOCUMENT CONTEXT AVAILABLE]", "unmatched query", "Full empty prompt", false);

        when(ragRetrievalService.retrieveAndAssembleContext("unmatched query", 1L, 5, testUser)).thenReturn(emptyContext);
        when(ragPromptBuilder.buildPrompt("unmatched query", emptyContext)).thenReturn(emptyPrompt);

        ragGenerationService.setMockResponseText("I could not find relevant information in the available documents.");

        RAGResponse response = ragGenerationService.generateAnswer("unmatched query", 1L, 5, testUser);

        assertNotNull(response);
        assertFalse(response.hasContext());
        assertEquals(0, response.citations().size());
        assertEquals(0, response.sources().size());
        assertEquals("I could not find relevant information in the available documents.", response.answer());
    }

    @Test
    @DisplayName("TEST 4: Gemini API failure throws controlled RuntimeException")
    void testGeminiApiFailure() {
        RAGContext ragContext = RAGContext.empty("query", 1L);
        RAGPrompt ragPrompt = new RAGPrompt("System", "Context", "query", "Prompt", false);

        when(ragRetrievalService.retrieveAndAssembleContext("query", 1L, 5, testUser)).thenReturn(ragContext);
        when(ragPromptBuilder.buildPrompt("query", ragContext)).thenReturn(ragPrompt);

        ragGenerationService.setErrorToThrow(new RuntimeException("Gemini generation provider returned error (HTTP 500)"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                ragGenerationService.generateAnswer("query", 1L, 5, testUser));
        assertTrue(ex.getMessage().contains("Gemini generation provider returned error"));
    }

    @Test
    @DisplayName("TEST 5: Missing Gemini API key throws controlled exception")
    void testMissingApiKey() {
        RAGGenerationServiceImpl realService = new RAGGenerationServiceImpl(ragRetrievalService, ragPromptBuilder);
        ReflectionTestUtils.setField(realService, "geminiApiKey", "");

        RAGContext ragContext = RAGContext.empty("query", 1L);
        RAGPrompt ragPrompt = new RAGPrompt("System", "Context", "query", "Prompt", false);

        when(ragRetrievalService.retrieveAndAssembleContext("query", 1L, 5, testUser)).thenReturn(ragContext);
        when(ragPromptBuilder.buildPrompt("query", ragContext)).thenReturn(ragPrompt);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                realService.generateAnswer("query", 1L, 5, testUser));
        assertTrue(ex.getMessage().contains("Gemini API key is not configured"));
    }

    @Test
    @DisplayName("TEST 6: Null or blank query throws IllegalArgumentException")
    void testBlankQueryValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                ragGenerationService.generateAnswer(null, 1L, 5, testUser));
        assertThrows(IllegalArgumentException.class, () ->
                ragGenerationService.generateAnswer("", 1L, 5, testUser));
        assertThrows(IllegalArgumentException.class, () ->
                ragGenerationService.generateAnswer("   ", 1L, 5, testUser));
    }
}
