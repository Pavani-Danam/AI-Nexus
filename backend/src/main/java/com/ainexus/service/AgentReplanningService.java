package com.ainexus.service;

import com.ainexus.dto.AgentExecutionResult;
import com.ainexus.dto.AgentPlan;
import com.ainexus.dto.ConversationMemory;
import com.ainexus.entity.User;

public interface AgentReplanningService {

    boolean shouldReplan(AgentPlan currentPlan, AgentExecutionResult executionResult);

    AgentPlan generateCorrectedPlan(
            AgentPlan failedPlan,
            AgentExecutionResult executionResult,
            int replanAttempt,
            ConversationMemory memory,
            User authenticatedUser
    );
}
