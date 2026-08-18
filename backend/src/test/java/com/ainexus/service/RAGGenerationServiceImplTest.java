package com.ainexus.service;

import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGCitation;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RAGGenerationServiceImplTest {

    @Mock
    private RAGRetrievalService retrievalService;

    @Mock
    private RAGPromptBuilder promptBuilder;

    @Mock
    private SemanticCacheService semanticCacheService;

    private TestableRAGGenerationServiceImpl ragGenerationService;
    private User testUser;

    static class TestableRAGGenerationServiceImpl extends RAGGenerationServiceImpl {
        private String mockGeminiResponse = "Default mock answer";
        private boolean shouldThrow = false;

        public TestableRAGGenerationServiceImpl(RAGRetrievalService retrievalService,
                                                RAGPromptBuilder promptBuilder) {
            super(retrievalService, promptBuilder);
        }

        public void setMockGeminiResponse(String response) {
            this.mockGeminiResponse = response;
            this.shouldThrow = false;
        }

        public void setShouldThrow(boolean shouldThrow) {
            this.shouldThrow = shouldThrow;
        }

        @Override
        protected String callGeminiGenerateContent(String promptText) {
            if (shouldThrow) {
                throw new RuntimeException("Simulated Gemini failure");
            }
            return mockGeminiResponse;
        }
    }

    @BeforeEach
    void setUp() {
        ragGenerationService = new TestableRAGGenerationServiceImpl(retrievalService, promptBuilder);
        ragGenerationService.setSemanticCacheService(semanticCacheService);
        ReflectionTestUtils.setField(ragGenerationService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(ragGenerationService, "generationModel", "gemini-1.5-flash");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: Semantic Cache Hit returns cached response immediately without calling retrieval")
    void testSemanticCacheHitShortCircuit() {
        RAGResponse cached = new RAGResponse(
                "Cached: 20 days annual leave.",
                "leave policy",
                1L,
                List.of(new RAGCitation(1L, "handbook.pdf", 0, 0.9, "doc-1-chunk-0", "20 days annual leave.")),
                Collections.emptyList(),
                true
        );

        when(semanticCacheService.lookup("leave policy", 1L, testUser))
                .thenReturn(Optional.of(cached));

        RAGResponse result = ragGenerationService.generateAnswer("leave policy", 1L, null, testUser);

        assertNotNull(result);
        assertEquals("Cached: 20 days annual leave.", result.answer());
        verify(retrievalService, never()).retrieveAndAssembleContext(any(), any(), any(), any());
    }

    @Test
    @DisplayName("TEST 2: Semantic Cache Miss executes retrieval and stores new response in cache")
    void testSemanticCacheMissPipelineExecution() {
        when(semanticCacheService.lookup("architecture", 1L, testUser))
                .thenReturn(Optional.empty());

        RAGChunk chunk = new RAGChunk(10L, "arch.pdf", 0, 0.9, "AI-Nexus microservices.", 22);
        RAGContext ragContext = new RAGContext("architecture", 1L, List.of(chunk), "[1] AI-Nexus microservices.", 22);

        when(retrievalService.retrieveAndAssembleContext(eq("architecture"), eq(1L), any(), eq(testUser)))
                .thenReturn(ragContext);
        when(promptBuilder.buildPrompt(eq("architecture"), eq(ragContext)))
                .thenReturn(new RAGPrompt("System instructions", "[1] AI-Nexus microservices.", "architecture", "Full prompt", true));

        ragGenerationService.setMockGeminiResponse("AI-Nexus is built with microservices.");

        RAGResponse result = ragGenerationService.generateAnswer("architecture", 1L, null, testUser);

        assertNotNull(result);
        assertEquals("AI-Nexus is built with microservices.", result.answer());
        assertTrue(result.hasContext());
        verify(semanticCacheService, times(1)).store(eq("architecture"), eq(1L), eq(testUser), any(RAGResponse.class));
    }

    @Test
    @DisplayName("TEST 3: Null or blank query throws IllegalArgumentException")
    void testInvalidQuery() {
        assertThrows(IllegalArgumentException.class, () ->
                ragGenerationService.generateAnswer(null, 1L, null, testUser));
        assertThrows(IllegalArgumentException.class, () ->
                ragGenerationService.generateAnswer("   ", 1L, null, testUser));
    }

    @Test
    @DisplayName("TEST 4: Null workspaceId throws IllegalArgumentException")
    void testNullWorkspace() {
        assertThrows(IllegalArgumentException.class, () ->
                ragGenerationService.generateAnswer("query", null, null, testUser));
    }

    @Test
    @DisplayName("TEST 5: Gemini failure throws RuntimeException without storing corrupted cache")
    void testGeminiFailureHandling() {
        when(semanticCacheService.lookup("query", 1L, testUser))
                .thenReturn(Optional.empty());

        RAGChunk chunk = new RAGChunk(10L, "doc.pdf", 0, 0.9, "Content", 7);
        RAGContext ragContext = new RAGContext("query", 1L, List.of(chunk), "[1] Content", 7);
        when(retrievalService.retrieveAndAssembleContext(eq("query"), eq(1L), any(), eq(testUser)))
                .thenReturn(ragContext);
        when(promptBuilder.buildPrompt(eq("query"), eq(ragContext)))
                .thenReturn(new RAGPrompt("System", "Content", "query", "Full", true));

        ragGenerationService.setShouldThrow(true);

        assertThrows(RuntimeException.class, () ->
                ragGenerationService.generateAnswer("query", 1L, null, testUser));
        verify(semanticCacheService, never()).store(any(), any(), any(), any());
    }
}
