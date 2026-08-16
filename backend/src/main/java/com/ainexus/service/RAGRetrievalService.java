package com.ainexus.service;

import com.ainexus.dto.RAGContext;
import com.ainexus.entity.User;

public interface RAGRetrievalService {
    RAGContext retrieveAndAssembleContext(String query, Long workspaceId, Integer topK, User authenticatedUser);
}
