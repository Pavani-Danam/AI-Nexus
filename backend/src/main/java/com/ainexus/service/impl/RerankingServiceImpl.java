package com.ainexus.service.impl;

import com.ainexus.dto.SearchResultItem;
import com.ainexus.service.RerankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RerankingServiceImpl implements RerankingService {

    private static final Logger logger = LoggerFactory.getLogger(RerankingServiceImpl.class);

    @Value("${app.rag.reranking.enabled:true}")
    private boolean rerankingEnabled;

    @Value("${app.rag.reranking.max-results:10}")
    private int maxResults;

    @Override
    public List<SearchResultItem> rerank(String query, List<SearchResultItem> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        if (!rerankingEnabled) {
            logger.debug("Reranking disabled. Returning candidates in original order bounded by maxResults.");
            return candidates.stream().limit(maxResults).collect(Collectors.toList());
        }

        Set<String> queryTerms = extractQueryTerms(query);

        List<ScoredCandidate> scoredList = new ArrayList<>();
        for (SearchResultItem item : candidates) {
            if (item != null) {
                double vectorScore = item.score() != null ? item.score() : 0.0;
                double lexicalScore = calculateLexicalOverlapScore(item.content(), queryTerms);
                
                // Deterministic hybrid score: 75% vector similarity + 25% lexical term coverage
                double finalScore = (0.75 * vectorScore) + (0.25 * lexicalScore);

                SearchResultItem updatedItem = new SearchResultItem(
                        item.documentId(),
                        item.filename(),
                        item.chunkIndex(),
                        Math.round(finalScore * 10000.0) / 10000.0,
                        item.content(),
                        item.characterCount(),
                        item.fileType(),
                        item.vectorId()
                );

                scoredList.add(new ScoredCandidate(updatedItem, finalScore));
            }
        }

        // Sort deterministically: highest score first, tie-break by documentId, then chunkIndex
        List<SearchResultItem> reranked = scoredList.stream()
                .sorted((a, b) -> {
                    int scoreCmp = Double.compare(b.finalScore(), a.finalScore());
                    if (scoreCmp != 0) {
                        return scoreCmp;
                    }
                    Long docA = a.item().documentId() != null ? a.item().documentId() : 0L;
                    Long docB = b.item().documentId() != null ? b.item().documentId() : 0L;
                    int docCmp = docA.compareTo(docB);
                    if (docCmp != 0) {
                        return docCmp;
                    }
                    Integer chunkA = a.item().chunkIndex() != null ? a.item().chunkIndex() : 0;
                    Integer chunkB = b.item().chunkIndex() != null ? b.item().chunkIndex() : 0;
                    return chunkA.compareTo(chunkB);
                })
                .map(ScoredCandidate::item)
                .limit(maxResults)
                .collect(Collectors.toList());

        logger.debug("Reranked {} candidates into {} ordered results for query: '{}'",
                candidates.size(), reranked.size(), query);

        return reranked;
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

    private double calculateLexicalOverlapScore(String content, Set<String> queryTerms) {
        if (content == null || content.isEmpty() || queryTerms.isEmpty()) {
            return 0.0;
        }

        String lowerContent = content.toLowerCase();
        int matched = 0;
        for (String term : queryTerms) {
            if (lowerContent.contains(term)) {
                matched++;
            }
        }

        return (double) matched / queryTerms.size();
    }

    private boolean isStopWord(String word) {
        return switch (word) {
            case "the", "and", "for", "with", "what", "where", "which", "how", "are", "this", "that", "from" -> true;
            default -> false;
        };
    }

    private record ScoredCandidate(SearchResultItem item, double finalScore) {}
}
