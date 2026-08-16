package com.ainexus.agent;

public class AgentException extends RuntimeException {

    private final String traceId;
    private final AgentType agentType;

    public AgentException(String message, AgentType agentType, String traceId) {
        super(message);
        this.agentType = agentType;
        this.traceId = traceId;
    }

    public AgentException(String message, AgentType agentType, String traceId, Throwable cause) {
        super(message, cause);
        this.agentType = agentType;
        this.traceId = traceId;
    }

    public String getTraceId() {
        return traceId;
    }

    public AgentType getAgentType() {
        return agentType;
    }
}
