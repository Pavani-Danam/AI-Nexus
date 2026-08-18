package com.ainexus.service;

import com.ainexus.entity.Conversation;
import com.ainexus.entity.Message;
import com.ainexus.entity.User;

import java.util.List;

public interface ConversationSummaryService {

    /**
     * Evaluates whether the conversation requires summarization and updates the
     * conversation's stored summary if older messages exceed the configured threshold.
     */
    String getOrUpdateSummary(Conversation conversation, List<Message> allChronologicalMessages, User authenticatedUser);

    /**
     * Creates a condensed summary of the given older messages incorporating any existing summary.
     */
    String generateSummary(String existingSummary, List<Message> olderMessagesToSummarize);
}
