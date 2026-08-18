package com.ainexus.service.impl;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.dto.MemoryMessage;
import com.ainexus.entity.User;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.service.ConversationMemoryService;
import com.ainexus.service.MemoryRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MemoryRetrievalServiceImpl implements MemoryRetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryRetrievalServiceImpl.class);

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-zA-Z0-9]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
            "to", "was", "were", "will", "with", "i", "you", "my", "me", "what",
            "which", "who", "whom", "this", "these", "those", "how", "why"
    );

    @Value("${app.chat.memory.max-relevant-messages:6}")
    private int maxRelevantMessages;

    private final ConversationMemoryService conversationMemoryService;

    public MemoryRetrievalServiceImpl(ConversationMemoryService conversationMemoryService) {
        this.conversationMemoryService = conversationMemoryService;
    }

    @Override
    public ConversationMemory retrieveRelevantMemory(String currentUserQuery, Long conversationId, Long workspaceId, User user) {
        if (currentUserQuery == null || currentUserQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Current user query must not be blank.");
        }
        if (user == null) {
            throw new UnauthorizedAccessException("Authenticated user required.");
        }
        if (conversationId == null) {
            return ConversationMemory.empty(null, workspaceId);
        }

        ConversationMemory fullMemory = conversationMemoryService.getMemory(conversationId, workspaceId, user);
        return filterRelevantMemory(currentUserQuery, fullMemory);
    }

    @Override
    public ConversationMemory filterRelevantMemory(String currentUserQuery, ConversationMemory fullMemory) {
        if (fullMemory == null || !fullMemory.hasHistory()) {
            return fullMemory != null ? fullMemory : ConversationMemory.empty(null, null);
        }

        String query = (currentUserQuery != null) ? currentUserQuery.trim() : "";
        Set<String> queryTokens = extractTokens(query);

        List<MemoryMessage> rawMessages = fullMemory.messages();
        if (rawMessages.isEmpty()) {
            return fullMemory;
        }

        // Score each message based on semantic/lexical overlap + modest recency weighting
        int total = rawMessages.size();
        List<ScoredMessage> scored = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            MemoryMessage msg = rawMessages.get(i);
            double lexicalScore = computeOverlapScore(queryTokens, extractTokens(msg.content()));
            // Modest recency boost (0.0 to 0.15) to break ties or favor immediate context
            double recencyBoost = ((double) (i + 1) / total) * 0.15;
            double finalScore = lexicalScore + (lexicalScore > 0 ? recencyBoost : 0.05 * recencyBoost);

            scored.add(new ScoredMessage(msg, i, finalScore, lexicalScore > 0));
        }

        // Filter and select top relevant messages
        boolean hasDirectMatches = scored.stream().anyMatch(s -> s.hasDirectMatch);

        List<ScoredMessage> selected;
        if (hasDirectMatches) {
            // Pick direct matches first, then fill with recent immediate context up to maxRelevantMessages
            selected = scored.stream()
                    .filter(s -> s.hasDirectMatch || s.originalIndex >= total - 2) // keep last 2 messages if available
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .limit(maxRelevantMessages)
                    .sorted(Comparator.comparingInt(a -> a.originalIndex)) // restore chronological order
                    .collect(Collectors.toList());
        } else {
            // Fallback: If no direct keywords matched (e.g., pure pronoun queries), keep the most recent window
            int start = Math.max(0, total - maxRelevantMessages);
            selected = scored.subList(start, total);
        }

        List<MemoryMessage> filteredMessages = selected.stream()
                .map(s -> s.message)
                .collect(Collectors.toList());

        // Extract and evaluate summary if present in formattedHistory
        String summary = extractSummaryFromFormattedHistory(fullMemory.formattedHistory());
        String formattedContext = formatMemoryContext(summary, filteredMessages);

        logger.info("Memory relevance selection: Retained {} of {} messages for query '{}'",
                filteredMessages.size(), total, query);

        return new ConversationMemory(
                fullMemory.conversationId(),
                fullMemory.workspaceId(),
                filteredMessages,
                formattedContext,
                filteredMessages.size()
        );
    }

    private double computeOverlapScore(Set<String> queryTokens, Set<String> docTokens) {
        if (queryTokens.isEmpty() || docTokens.isEmpty()) {
            return 0.0;
        }
        long matches = queryTokens.stream().filter(docTokens::contains).count();
        return (double) matches / queryTokens.size();
    }

    private Set<String> extractTokens(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(TOKEN_SPLIT.split(text.toLowerCase()))
                .filter(t -> t.length() > 2 && !STOP_WORDS.contains(t))
                .collect(Collectors.toSet());
    }

    private String extractSummaryFromFormattedHistory(String history) {
        if (history == null || !history.contains("CONVERSATION SUMMARY:")) {
            return null;
        }
        try {
            int start = history.indexOf("CONVERSATION SUMMARY:") + "CONVERSATION SUMMARY:".length();
            int end = history.contains("RECENT MESSAGES:") ? history.indexOf("RECENT MESSAGES:") : history.length();
            return history.substring(start, end).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String formatMemoryContext(String summary, List<MemoryMessage> messages) {
        StringBuilder sb = new StringBuilder();

        if (summary != null && !summary.isBlank()) {
            sb.append("CONVERSATION SUMMARY:\n")
              .append(summary.trim())
              .append("\n\n");
        }

        if (messages != null && !messages.isEmpty()) {
            if (summary != null && !summary.isBlank()) {
                sb.append("RECENT MESSAGES:\n");
            }
            for (MemoryMessage msg : messages) {
                sb.append(msg.role()).append(":\n")
                  .append(msg.content()).append("\n\n");
            }
        }

        return sb.toString().trim();
    }

    private static class ScoredMessage {
        final MemoryMessage message;
        final int originalIndex;
        final double score;
        final boolean hasDirectMatch;

        ScoredMessage(MemoryMessage message, int originalIndex, double score, boolean hasDirectMatch) {
            this.message = message;
            this.originalIndex = originalIndex;
            this.score = score;
            this.hasDirectMatch = hasDirectMatch;
        }
    }
}
