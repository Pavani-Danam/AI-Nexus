package com.ainexus.service.impl;

import com.ainexus.dto.SearchResultItem;
import com.ainexus.service.ContextCompressionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ContextCompressionServiceImpl implements ContextCompressionService {

    private static final Logger logger = LoggerFactory.getLogger(ContextCompressionServiceImpl.class);
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[.!?])\\s+");

    @Value("${app.rag.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${app.rag.compression.min-sentence-relevance:0.20}")
    private double minSentenceRelevance;

    @Override
    public List<SearchResultItem> compressContext(String query, List<SearchResultItem> candidateChunks) {
        if (candidateChunks == null || candidateChunks.isEmpty()) {
            return Collections.emptyList();
        }

        if (!compressionEnabled) {
            logger.debug("Context compression disabled. Returning original candidate chunks.");
            return new ArrayList<>(candidateChunks);
        }

        Set<String> queryTerms = extractQueryTerms(query);
        if (queryTerms.isEmpty()) {
            return new ArrayList<>(candidateChunks);
        }

        List<SearchResultItem> compressedChunks = new ArrayList<>();

        for (SearchResultItem item : candidateChunks) {
            if (item != null && item.content() != null && !item.content().trim().isEmpty()) {
                String compressedContent = compressChunkText(item.content(), queryTerms);

                // If compression filtered too aggressively, fall back to the original content
                if (compressedContent.trim().isEmpty()) {
                    compressedContent = item.content().trim();
                }

                SearchResultItem compressedItem = new SearchResultItem(
                        item.documentId(),
                        item.filename(),
                        item.chunkIndex(),
                        item.score(),
                        compressedContent,
                        compressedContent.length(),
                        item.fileType(),
                        item.vectorId()
                );
                compressedChunks.add(compressedItem);
            }
        }

        logger.debug("Compressed {} chunks for query: '{}'", compressedChunks.size(), query);
        return compressedChunks;
    }

    private String compressChunkText(String content, Set<String> queryTerms) {
        String[] sentences = SENTENCE_SPLIT_PATTERN.split(content.trim());
        if (sentences.length <= 1) {
            return content.trim();
        }

        List<String> retainedSentences = new ArrayList<>();
        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (!trimmedSentence.isEmpty()) {
                double relevance = calculateSentenceRelevance(trimmedSentence, queryTerms);
                if (relevance >= minSentenceRelevance) {
                    retainedSentences.add(trimmedSentence);
                }
            }
        }

        if (retainedSentences.isEmpty()) {
            return content.trim();
        }

        return String.join(" ", retainedSentences);
    }

    private double calculateSentenceRelevance(String sentence, Set<String> queryTerms) {
        if (sentence == null || sentence.isEmpty() || queryTerms.isEmpty()) {
            return 0.0;
        }

        String lowerSentence = sentence.toLowerCase();
        int matchCount = 0;
        for (String term : queryTerms) {
            if (lowerSentence.contains(term)) {
                matchCount++;
            }
        }

        return (double) matchCount / queryTerms.size();
    }

    private Set<String> extractQueryTerms(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptySet();
        }
        String[] tokens = query.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        Set<String> terms = new HashSet<>();
        for (String token : tokens) {
            if (token.length() >= 3 && !isStopWord(token)) {
                terms.add(token);
            }
        }
        return terms;
    }

    private boolean isStopWord(String word) {
        return switch (word) {
            case "the", "and", "for", "with", "what", "where", "which", "how", "are", "this", "that", "from" -> true;
            default -> false;
        };
    }
}
