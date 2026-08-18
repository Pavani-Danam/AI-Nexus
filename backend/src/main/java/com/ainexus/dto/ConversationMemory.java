package com.ainexus.dto;

import java.util.Collections;
import java.util.List;

public record ConversationMemory(
        Long conversationId,
        Long workspaceId,
        List<MemoryMessage> messages,
        String formattedHistory,
        int messageCount
) {
    public static ConversationMemory empty(Long conversationId, Long workspaceId) {
        return new ConversationMemory(conversationId, workspaceId, Collections.emptyList(), "", 0);
    }

    public boolean hasHistory() {
        return messages != null && !messages.isEmpty();
    }
}
