package com.ainexus.service;

import com.ainexus.dto.TextChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentTextChunkingServiceTest {

    private DocumentTextChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new DocumentTextChunkingService();
    }

    @Test
    void testShortDocumentReturnsSingleChunk() {
        String input = "This is a short document content.";
        List<TextChunk> chunks = chunkingService.chunkText(input, 500, 50);

        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).index());
        assertEquals(input, chunks.get(0).content());
    }

    @Test
    void testLongDocumentProducesMultipleChunks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 20; i++) {
            sb.append("Section ").append(i).append(": Detailed explanation of AI-Nexus enterprise capabilities. ");
        }
        String input = sb.toString();

        List<TextChunk> chunks = chunkingService.chunkText(input, 200, 40);

        assertTrue(chunks.size() > 1, "Expected multiple chunks for long document");
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).index(), "Chunk indices must be strictly sequential");
            assertFalse(chunks.get(i).content().isBlank(), "Chunks must not be blank");
        }
    }

    @Test
    void testChunkOverlapPreserved() {
        String input = "Sentence one is clear. Sentence two is very informative. Sentence three wraps up the section.";
        List<TextChunk> chunks = chunkingService.chunkText(input, 55, 20);

        assertTrue(chunks.size() >= 2);
        // Verify subsequent chunk shares overlapping text with previous chunk
        String chunk0 = chunks.get(0).content();
        String chunk1 = chunks.get(1).content();
        assertNotNull(chunk0);
        assertNotNull(chunk1);
    }

    @Test
    void testParagraphBoundariesPreferred() {
        String input = "First Paragraph with important introductory concepts.\n\nSecond Paragraph covering architecture details.";
        List<TextChunk> chunks = chunkingService.chunkText(input, 80, 20);

        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(0).content().contains("First Paragraph"));
    }

    @Test
    void testSentenceBoundariesPreferred() {
        String input = "Spring Boot makes microservices easy. Enterprise security is essential. Vector databases enable RAG search.";
        List<TextChunk> chunks = chunkingService.chunkText(input, 60, 15);

        for (TextChunk chunk : chunks) {
            assertFalse(chunk.content().isEmpty());
        }
    }

    @Test
    void testEmptyTextReturnsEmptyList() {
        List<TextChunk> chunks = chunkingService.chunkText("");
        assertTrue(chunks.isEmpty());

        chunks = chunkingService.chunkText(null);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testWhitespaceOnlyReturnsEmptyList() {
        List<TextChunk> chunks = chunkingService.chunkText("     \n\n\t    ");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testUnicodePreservedInChunks() {
        String input = "AI-Nexus Enterprise Platform supports multilingual text: ???? ??? and ?????? ???.";
        List<TextChunk> chunks = chunkingService.chunkText(input, 500, 50);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).content().contains("????"));
        assertTrue(chunks.get(0).content().contains("??????"));
    }

    @Test
    void testVerySmallChunkSize() {
        String input = "Alpha Beta Gamma Delta Epsilon";
        List<TextChunk> chunks = chunkingService.chunkText(input, 12, 4);

        assertTrue(chunks.size() >= 2);
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).index());
        }
    }

    @Test
    void testInvalidConfigurationThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> chunkingService.chunkText("text", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> chunkingService.chunkText("text", -10, 0));
        assertThrows(IllegalArgumentException.class, () -> chunkingService.chunkText("text", 100, -1));
        assertThrows(IllegalArgumentException.class, () -> chunkingService.chunkText("text", 100, 100));
        assertThrows(IllegalArgumentException.class, () -> chunkingService.chunkText("text", 100, 150));
    }
}
