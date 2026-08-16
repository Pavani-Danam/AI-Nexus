package com.ainexus.service;

import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;

public interface RAGGenerationService {
    RAGResponse generateAnswer(String query, Long workspaceId, Integer topK, User authenticatedUser);
}
