package com.ainexus.service;

import com.ainexus.dto.AgentExecutionResult;
import com.ainexus.dto.AgentPlan;
import com.ainexus.dto.ConversationMemory;
import com.ainexus.entity.User;

public interface PlanExecutionService {

    AgentExecutionResult executePlan(AgentPlan plan, User authenticatedUser);

    AgentExecutionResult executePlan(AgentPlan plan, ConversationMemory memory, User authenticatedUser);
}
