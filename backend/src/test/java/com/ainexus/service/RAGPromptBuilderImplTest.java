package com.ainexus.service;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.dto.MemoryMessage;
import com.ainexus.dto.RAGChunk;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.RAGPrompt;
import com.ainexus.service.impl.RAGPromptBuilderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RAGPromptBuilderImplTest {

    private RAGPromptBuilderImpl promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new RAGPromptBuilderImpl();
    }

    @Test
    @DisplayName("TEST 1: Build prompt with both RAG context and Conversation Memory")
    void testBuildPromptWithContextAndMemory() {
        RAGChunk chunk = new RAGChunk(1L, "policy.pdf", 1, 0.95, "Employees get 20 days annual leave.", 35);
        RAGContext context = new RAGContext("How many days leave?", 10L, List.of(chunk), "Context string", 1);

        List<MemoryMessage> messages = List.of(
                MemoryMessage.of(1L, "USER", "Tell me about leave policy", LocalDateTime.now().minusMinutes(2)),
                MemoryMessage.of(2L, "ASSISTANT", "Sure, what would you like to know?", LocalDateTime.now().minusMinutes(1))
        );
        String formattedMemory = "USER:\nTell me about leave policy\n\nASSISTANT:\nSure, what would you like to know?";
        ConversationMemory memory = new ConversationMemory(100L, 10L, messages, formattedMemory, 2);

        RAGPrompt prompt = promptBuilder.buildPrompt("How many days leave?", context, memory);

        assertNotNull(prompt);
        assertTrue(prompt.hasContext());
        assertEquals("How many days leave?", prompt.userQuestion());
        assertTrue(prompt.retrievedContext().contains("--- BEGIN CONVERSATION CONTEXT ---"));
        assertTrue(prompt.retrievedContext().contains("Tell me about leave policy"));
        assertTrue(prompt.retrievedContext().contains("--- BEGIN AUTHORITATIVE KNOWLEDGE CONTEXT ---"));
        assertTrue(prompt.retrievedContext().contains("Employees get 20 days annual leave."));
        assertTrue(prompt.fullPrompt().contains("CURRENT QUESTION:\nHow many days leave?"));
    }

    @Test
    @DisplayName("TEST 2: Build prompt without Conversation Memory (Normal query)")
    void testBuildPromptWithoutMemory() {
        RAGChunk chunk = new RAGChunk(1L, "handbook.pdf", 1, 0.88, "Office hours are 9 to 5.", 24);
        RAGContext context = new RAGContext("What are the office hours?", 10L, List.of(chunk), "Context", 1);

        RAGPrompt prompt = promptBuilder.buildPrompt("What are the office hours?", context, null);

        assertNotNull(prompt);
        assertTrue(prompt.hasContext());
        assertEquals("What are the office hours?", prompt.userQuestion());
        assertFalse(prompt.retrievedContext().contains("--- BEGIN CONVERSATION CONTEXT ---"));
        assertTrue(prompt.retrievedContext().contains("--- BEGIN AUTHORITATIVE KNOWLEDGE CONTEXT ---"));
        assertTrue(prompt.retrievedContext().contains("Office hours are 9 to 5."));
    }

    @Test
    @DisplayName("TEST 3: Build prompt with empty RAG context")
    void testBuildPromptWithEmptyContext() {
        RAGContext emptyContext = new RAGContext("Unknown query", 10L, Collections.emptyList(), "", 0);

        RAGPrompt prompt = promptBuilder.buildPrompt("Unknown query", emptyContext, null);

        assertNotNull(prompt);
        assertFalse(prompt.hasContext());
        assertTrue(prompt.retrievedContext().contains("--- NO AUTHORITATIVE KNOWLEDGE RETRIEVED ---"));
    }

    @Test
    @DisplayName("TEST 4: Blank query throws IllegalArgumentException")
    void testBlankQueryThrowsException() {
        RAGContext context = new RAGContext("", 10L, Collections.emptyList(), "", 0);
        assertThrows(IllegalArgumentException.class, () -> promptBuilder.buildPrompt("   ", context));
    }
}
