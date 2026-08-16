package com.ainexus.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentTextCleaningServiceTest {

    private DocumentTextCleaningService cleaningService;

    @BeforeEach
    void setUp() {
        cleaningService = new DocumentTextCleaningService();
    }

    @Test
    void testMultipleSpacesCollapsed() {
        String input = "AI-Nexus    Enterprise    Platform";
        String expected = "AI-Nexus Enterprise Platform";
        assertEquals(expected, cleaningService.cleanText(input));
    }

    @Test
    void testMultipleBlankLinesReduced() {
        String input = "Paragraph 1\n\n\n\n\nParagraph 2";
        String expected = "Paragraph 1\n\nParagraph 2";
        assertEquals(expected, cleaningService.cleanText(input));
    }

    @Test
    void testDifferentNewlineFormats() {
        String input = "Line 1\r\nLine 2\rLine 3\nLine 4";
        String expected = "Line 1\nLine 2\nLine 3\nLine 4";
        assertEquals(expected, cleaningService.cleanText(input));
    }

    @Test
    void testLeadingAndTrailingWhitespace() {
        String input = "   \n\t  AI-Nexus Document Content   \n\t  ";
        String expected = "AI-Nexus Document Content";
        assertEquals(expected, cleaningService.cleanText(input));
    }

    @Test
    void testEmptyContentThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> cleaningService.cleanText("      "));
        assertThrows(IllegalArgumentException.class, () -> cleaningService.cleanText("\n\n\n"));
        assertThrows(IllegalArgumentException.class, () -> cleaningService.cleanText("   \n   \n\t  "));
        assertThrows(IllegalArgumentException.class, () -> cleaningService.cleanText(null));
    }

    @Test
    void testUnicodePreserved() {
        String input = "  AI-Nexus   ????    ??????  ";
        String expected = "AI-Nexus ???? ??????";
        assertEquals(expected, cleaningService.cleanText(input));
    }

    @Test
    void testPunctuationPreserved() {
        String input = "AI-Nexus: Enterprise AI Platform; features: [Search, RAG, Chat]? Yes!";
        String expected = "AI-Nexus: Enterprise AI Platform; features: [Search, RAG, Chat]? Yes!";
        assertEquals(expected, cleaningService.cleanText(input));
    }

    @Test
    void testParagraphStructurePreserved() {
        String input = "First Paragraph with   extra spaces.\n\nSecond Paragraph remains distinct.";
        String expected = "First Paragraph with extra spaces.\n\nSecond Paragraph remains distinct.";
        assertEquals(expected, cleaningService.cleanText(input));
    }
}
