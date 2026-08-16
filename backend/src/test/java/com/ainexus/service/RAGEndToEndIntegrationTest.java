package com.ainexus.service;

import com.ainexus.dto.*;
import com.ainexus.entity.User;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RAGEndToEndIntegrationTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    private RAGRetrievalService ragRetrievalService;
    private RAGPromptBuilder ragPromptBuilder;
    private TestableRAGGenerationService ragGenerationService;

    private User userA;
    private User userB;

    static class TestableRAGGenerationService extends RAGGenerationServiceImpl {
        private String mockGeminiAnswer = "Default grounded answer.";
        private String capturedPrompt = null;

        public TestableRAGGenerationService(RAGRetrievalService retrievalService, RAGPromptBuilder promptBuilder) {
            super(retrievalService, promptBuilder);
        }

        public void setMockGeminiAnswer(String answer) {
            this.mockGeminiAnswer = answer;
        }

        public String getCapturedPrompt() {
            return capturedPrompt;
        }

        @Override
        protected String callGeminiGenerateContent(String promptText) {
            this.capturedPrompt = promptText;
            return mockGeminiAnswer;
        }
    }

    @BeforeEach
    void setUp() {
        ragRetrievalService = new RAGRetrievalServiceImpl(semanticSearchService);
        ReflectionTestUtils.setField(ragRetrievalService, "defaultTopK", 5);
        ReflectionTestUtils.setField(ragRetrievalService, "minRelevanceScore", 0.35);
        ReflectionTestUtils.setField(ragRetrievalService, "maxContextCharacters", 4000);

        ragPromptBuilder = new RAGPromptBuilderImpl();

        ragGenerationService = new TestableRAGGenerationService(ragRetrievalService, ragPromptBuilder);
        ReflectionTestUtils.setField(ragGenerationService, "geminiApiKey", "valid-gemini-key");
        ReflectionTestUtils.setField(ragGenerationService, "generationModel", "gemini-1.5-flash");
        ReflectionTestUtils.setField(ragGenerationService, "temperature", 0.2);
        ReflectionTestUtils.setField(ragGenerationService, "maxOutputTokens", 2048);
        ReflectionTestUtils.setField(ragGenerationService, "timeoutSeconds", 30);

        userA = new User();
        userA.setId(1L);
        userA.setUsername("userA");

        userB = new User();
        userB.setId(2L);
        userB.setUsername("userB");
    }

    @Test
    @DisplayName("E2E STEP 1-8: Full Pipeline -> Retrieval -> Prompt Construction -> Gemini -> Grounded Answer with Citations")
    void testCompleteRAGPipelineSuccess() {
        String chunk1Content = "AI-Nexus features a resilient RAG pipeline integrating Pinecone and Google Gemini.";
        SearchResultItem chunk1 = new SearchResultItem(
                101L, "ai_nexus_overview.pdf", 0, 0.94,
                chunk1Content, chunk1Content.length(), "application/pdf", "doc_101_chunk_0"
        );

        String chunk2Content = "All chunk vectors are indexed with 768-dimension embeddings.";
        SearchResultItem chunk2 = new SearchResultItem(
                101L, "ai_nexus_overview.pdf", 1, 0.88,
                chunk2Content, chunk2Content.length(), "application/pdf", "doc_101_chunk_1"
        );

        SearchResponse searchResponse = new SearchResponse(
                "How does AI-Nexus RAG work?",
                10L,
                2,
                List.of(chunk1, chunk2)
        );

        when(semanticSearchService.search("How does AI-Nexus RAG work?", 10L, 5, userA))
                .thenReturn(searchResponse);

        ragGenerationService.setMockGeminiAnswer(
                "AI-Nexus implements a RAG pipeline utilizing Pinecone for vector storage and Google Gemini with 768-dimension embeddings."
        );

        // Execute generation
        RAGResponse response = ragGenerationService.generateAnswer("How does AI-Nexus RAG work?", 10L, 5, userA);

        // Assert Grounded Output
        assertNotNull(response);
        assertEquals("AI-Nexus implements a RAG pipeline utilizing Pinecone for vector storage and Google Gemini with 768-dimension embeddings.", response.answer());
        assertEquals("How does AI-Nexus RAG work?", response.query());
        assertEquals(10L, response.workspaceId());
        assertTrue(response.hasContext());

        // Assert Authoritative Sources and Citations
        assertEquals(2, response.citations().size());
        assertEquals("doc-101-chunk-0", response.citations().get(0).sourceId());
        assertEquals("ai_nexus_overview.pdf", response.citations().get(0).filename());
        assertEquals(0.94, response.citations().get(0).similarityScore());
        assertEquals("doc-101-chunk-1", response.citations().get(1).sourceId());

        // Assert Prompt Scaffolding and Injection Isolation
        String sentPrompt = ragGenerationService.getCapturedPrompt();
        assertNotNull(sentPrompt);
        assertTrue(sentPrompt.contains("=== SYSTEM INSTRUCTIONS ==="));
        assertTrue(sentPrompt.contains("=== RETRIEVED DOCUMENT CONTEXT ==="));
        assertTrue(sentPrompt.contains("=== USER QUESTION ==="));
        assertTrue(sentPrompt.contains("AI-Nexus features a resilient RAG pipeline"));
    }

    @Test
    @DisplayName("E2E STEP 9: Workspace Isolation -> User B cannot access Workspace A documents")
    void testWorkspaceIsolationVerification() {
        when(semanticSearchService.search("financial forecast", 20L, 5, userB))
                .thenReturn(new SearchResponse("financial forecast", 20L, 0, Collections.emptyList()));

        ragGenerationService.setMockGeminiAnswer("I do not have sufficient information in the available documents.");

        RAGResponse response = ragGenerationService.generateAnswer("financial forecast", 20L, 5, userB);

        assertNotNull(response);
        assertFalse(response.hasContext());
        assertTrue(response.citations().isEmpty());
        assertTrue(response.sources().isEmpty());
    }

    @Test
    @DisplayName("E2E STEP 10-11: Unrelated Query -> Safe Grounding (No Hallucinated Sources)")
    void testUnrelatedQueryNoHallucinations() {
        when(semanticSearchService.search("What is the recipe for chocolate cake?", 10L, 5, userA))
                .thenReturn(new SearchResponse("What is the recipe for chocolate cake?", 10L, 0, Collections.emptyList()));

        ragGenerationService.setMockGeminiAnswer(
                "The available documents do not contain information regarding chocolate cake recipes."
        );

        RAGResponse response = ragGenerationService.generateAnswer("What is the recipe for chocolate cake?", 10L, 5, userA);

        assertNotNull(response);
        assertFalse(response.hasContext());
        assertEquals(0, response.citations().size());
        assertEquals("The available documents do not contain information regarding chocolate cake recipes.", response.answer());
    }
}
