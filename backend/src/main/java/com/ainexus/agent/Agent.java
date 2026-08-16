package com.ainexus.agent;

public interface Agent {

    AgentType getAgentType();

    AgentResult execute(AgentRequest request, AgentContext context);

    default boolean supports(AgentType type) {
        return getAgentType() == type;
    }
}
