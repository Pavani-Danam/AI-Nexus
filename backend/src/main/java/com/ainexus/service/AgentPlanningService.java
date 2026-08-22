package com.ainexus.service;

import com.ainexus.dto.AgentPlan;
import com.ainexus.dto.ConversationMemory;
import com.ainexus.entity.User;

public interface AgentPlanningService {

    AgentPlan createPlan(String query, Long workspaceId, User authenticatedUser);

    AgentPlan createPlan(String query, Long workspaceId, Long conversationId, User authenticatedUser);

    AgentPlan createPlan(String query, Long workspaceId, ConversationMemory memory, User authenticatedUser);

    boolean validatePlan(AgentPlan plan);
}
