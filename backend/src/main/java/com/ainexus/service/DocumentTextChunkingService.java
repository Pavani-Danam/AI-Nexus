package com.ainexus.service;

import com.ainexus.dto.TextChunk;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DocumentTextChunkingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTextChunkingService.class);

    @Value("${app.rag.chunk-size:500}")
    private int defaultChunkSize = 500;

    @Value("${app.rag.chunk-overlap:100}")
    private int defaultChunkOverlap = 100;

    @PostConstruct
    public void validateDefaultConfig() {
        validateParameters(defaultChunkSize, defaultChunkOverlap);
        logger.info("Initialized DocumentTextChunkingService with default chunkSize={}, chunkOverlap={}",
                defaultChunkSize, defaultChunkOverlap);
    }

    public List<TextChunk> chunkText(String text) {
        return chunkText(text, defaultChunkSize, defaultChunkOverlap);
    }

    public List<TextChunk> chunkText(String text, int chunkSize, int chunkOverlap) {
        validateParameters(chunkSize, chunkOverlap);

        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String content = text.trim();
        if (content.isEmpty()) {
            return Collections.emptyList();
        }

        // Short document case: single chunk
        if (content.length() <= chunkSize) {
            return List.of(new TextChunk(0, content, 0, content.length()));
        }

        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        int textLength = content.length();
        int chunkIndex = 0;

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);

            if (end < textLength) {
                int boundary = findBestSplitBoundary(content, start, end);
                if (boundary > start) {
                    end = boundary;
                }
            }

            String chunkContent = content.substring(start, end).trim();
            if (!chunkContent.isEmpty()) {
                chunks.add(new TextChunk(chunkIndex++, chunkContent, start, end));
            }

            if (end >= textLength) {
                break;
            }

            // Advance pointer with overlap
            int step = Math.max(1, (end - start) - chunkOverlap);
            start += step;
        }

        return chunks;
    }

    private int findBestSplitBoundary(String text, int start, int targetEnd) {
        int minAcceptableBoundary = start + (int) ((targetEnd - start) * 0.4);

        // 1. Paragraph boundary
        int paragraphBreak = text.lastIndexOf("\n\n", targetEnd);
        if (paragraphBreak >= minAcceptableBoundary) {
            return paragraphBreak + 2;
        }

        // 2. Sentence boundary (. , ! , ?)
        for (int i = targetEnd - 1; i >= minAcceptableBoundary; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                if (i + 1 < text.length() && (Character.isWhitespace(text.charAt(i + 1)) || text.charAt(i + 1) == '\n')) {
                    return i + 1;
                }
            }
        }

        // 3. Newline boundary
        int singleNewline = text.lastIndexOf('\n', targetEnd);
        if (singleNewline >= minAcceptableBoundary) {
            return singleNewline + 1;
        }

        // 4. Word boundary
        int spaceIndex = text.lastIndexOf(' ', targetEnd);
        if (spaceIndex >= minAcceptableBoundary) {
            return spaceIndex + 1;
        }

        // Fallback: character boundary
        return targetEnd;
    }

    public void validateParameters(int chunkSize, int chunkOverlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than 0. Provided: " + chunkSize);
        }
        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("Chunk overlap cannot be negative. Provided: " + chunkOverlap);
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap (" + chunkOverlap + ") must be less than chunk size (" + chunkSize + ").");
        }
    }
}
