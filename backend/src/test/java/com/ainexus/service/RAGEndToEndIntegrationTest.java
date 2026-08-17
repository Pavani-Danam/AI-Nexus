package com.ainexus.service;

import com.ainexus.dto.EnhancedQuery;
import com.ainexus.dto.RAGResponse;
import com.ainexus.dto.SearchResultItem;
import com.ainexus.entity.User;
import com.ainexus.service.impl.ContextManagementServiceImpl;
import com.ainexus.service.impl.RAGGenerationServiceImpl;
import com.ainexus.service.impl.RAGPromptBuilderImpl;
import com.ainexus.service.impl.RAGRetrievalServiceImpl;
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
class RAGEndToEndIntegrationTest {

    @Mock
    private MultiQueryRetrievalService multiQueryRetrievalService;

    @Mock
    private QueryEnhancementService queryEnhancementService;

    private RAGGenerationServiceImpl ragGenerationService;
    private User testUser;

    static class TestableRAGGenerationServiceImpl extends RAGGenerationServiceImpl {
        private String mockGeminiResponse = "Default mock answer";

        public TestableRAGGenerationServiceImpl(RAGRetrievalService retrievalService,
                                                RAGPromptBuilder promptBuilder) {
            super(retrievalService, promptBuilder);
        }

        public void setMockGeminiResponse(String response) {
            this.mockGeminiResponse = response;
        }

        @Override
        protected String callGeminiGenerateContent(String promptText) {
            return mockGeminiResponse;
        }
    }

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("integrationUser");

        ContextManagementServiceImpl contextManagementService = new ContextManagementServiceImpl();
        ReflectionTestUtils.setField(contextManagementService, "minRelevanceScore", 0.35);
        ReflectionTestUtils.setField(contextManagementService, "maxContextCharacters", 8000);

        RAGRetrievalServiceImpl retrievalService = new RAGRetrievalServiceImpl(
                contextManagementService,
                queryEnhancementService,
                multiQueryRetrievalService
        );
        ReflectionTestUtils.setField(retrievalService, "defaultTopK", 5);

        RAGPromptBuilderImpl promptBuilder = new RAGPromptBuilderImpl();

        TestableRAGGenerationServiceImpl testableGenService = new TestableRAGGenerationServiceImpl(
                retrievalService,
                promptBuilder
        );
        ReflectionTestUtils.setField(testableGenService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(testableGenService, "generationModel", "gemini-1.5-flash");

        ragGenerationService = testableGenService;
    }

    @Test
    @DisplayName("E2E RAG TEST 1: Full multi-query pipeline produces grounded answer with citations")
    void testFullRAGPipelineSuccess() {
        when(queryEnhancementService.enhanceQuery("How does AI-Nexus RAG work?"))
                .thenReturn(EnhancedQuery.unchanged("How does AI-Nexus RAG work?"));

        SearchResultItem item1 = new SearchResultItem(101L, "rag_guide.pdf", 0, 0.94, "AI-Nexus uses vector retrieval for context assembly.", 50, "application/pdf", "v-1");
        SearchResultItem item2 = new SearchResultItem(102L, "rag_guide.pdf", 1, 0.88, "Chunks are fed into Gemini with strict grounding instructions.", 60, "application/pdf", "v-2");

        when(multiQueryRetrievalService.retrieveMultiQueryResults(eq("How does AI-Nexus RAG work?"), eq(10L), eq(5), eq(testUser)))
                .thenReturn(List.of(item1, item2));

        ((TestableRAGGenerationServiceImpl) ragGenerationService)
                .setMockGeminiResponse("AI-Nexus performs vector retrieval and feeds context to Gemini for grounded answers.");

        RAGResponse response = ragGenerationService.generateAnswer("How does AI-Nexus RAG work?", 10L, null, testUser);

        assertNotNull(response);
        assertEquals("AI-Nexus performs vector retrieval and feeds context to Gemini for grounded answers.", response.answer());
        assertTrue(response.hasContext());
        assertEquals(2, response.citations().size());
        assertEquals("rag_guide.pdf", response.citations().get(0).filename());
        assertEquals(0.94, response.citations().get(0).similarityScore());
    }

    @Test
    @DisplayName("E2E RAG TEST 2: Irrelevant or empty search produces controlled fallback without hallucination")
    void testFullRAGPipelineNoContextFallback() {
        when(queryEnhancementService.enhanceQuery("What is the recipe for chocolate cake?"))
                .thenReturn(EnhancedQuery.unchanged("What is the recipe for chocolate cake?"));

        when(multiQueryRetrievalService.retrieveMultiQueryResults(eq("What is the recipe for chocolate cake?"), eq(10L), eq(5), eq(testUser)))
                .thenReturn(Collections.emptyList());

        RAGResponse response = ragGenerationService.generateAnswer("What is the recipe for chocolate cake?", 10L, null, testUser);

        assertNotNull(response);
        assertFalse(response.hasContext());
        assertTrue(response.citations().isEmpty());
    }

    @Test
    @DisplayName("E2E RAG TEST 3: Multi-tenant workspace isolation across pipeline")
    void testWorkspaceIsolationAcrossPipeline() {
        when(queryEnhancementService.enhanceQuery("financial forecast"))
                .thenReturn(EnhancedQuery.unchanged("financial forecast"));

        when(multiQueryRetrievalService.retrieveMultiQueryResults(eq("financial forecast"), eq(20L), eq(5), eq(testUser)))
                .thenReturn(Collections.emptyList());

        RAGResponse response = ragGenerationService.generateAnswer("financial forecast", 20L, null, testUser);

        assertNotNull(response);
        assertFalse(response.hasContext());
        verify(multiQueryRetrievalService, never()).retrieveMultiQueryResults(eq("financial forecast"), eq(10L), any(), any());
    }
}
