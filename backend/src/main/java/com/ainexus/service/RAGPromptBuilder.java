package com.ainexus.service;

import com.ainexus.dto.ConversationMemory;
import com.ainexus.dto.RAGContext;
import com.ainexus.dto.RAGPrompt;

public interface RAGPromptBuilder {
    RAGPrompt buildPrompt(String userQuery, RAGContext ragContext);
    RAGPrompt buildPrompt(String userQuery, RAGContext ragContext, ConversationMemory memory);
}
