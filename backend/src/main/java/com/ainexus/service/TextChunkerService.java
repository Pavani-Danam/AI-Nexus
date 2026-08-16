package com.ainexus.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkerService {

    private static final int DEFAULT_CHUNK_SIZE = 500; // estimated words per chunk (~650-700 tokens)
    private static final int DEFAULT_CHUNK_OVERLAP = 50; // overlap words

    public record ChunkResult(int index, String content, int tokenCount) {}

    public List<ChunkResult> chunkText(String fullText) {
        return chunkText(fullText, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    }

    public List<ChunkResult> chunkText(String fullText, int chunkSize, int chunkOverlap) {
        List<ChunkResult> chunks = new ArrayList<>();
        if (fullText == null || fullText.trim().isEmpty()) {
            return chunks;
        }

        String[] words = fullText.split("\\s+");
        if (words.length == 0) {
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;

        while (start < words.length) {
            int end = Math.min(start + chunkSize, words.length);
            StringBuilder chunkBuilder = new StringBuilder();

            for (int i = start; i < end; i++) {
                if (i > start) {
                    chunkBuilder.append(" ");
                }
                chunkBuilder.append(words[i]);
            }

            String chunkContent = chunkBuilder.toString().trim();
            if (!chunkContent.isEmpty()) {
                // Rule-of-thumb: 1 word ~ 1.3 tokens
                int estimatedTokens = Math.max(1, (int) Math.ceil(chunkContent.split("\\s+").length * 1.3));
                chunks.add(new ChunkResult(chunkIndex++, chunkContent, estimatedTokens));
            }

            if (end == words.length) {
                break;
            }

            start += Math.max(1, chunkSize - chunkOverlap);
        }

        return chunks;
    }
}
