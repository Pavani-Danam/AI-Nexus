package com.ainexus.agent;

public enum AgentType {
    ORCHESTRATOR("Coordinates workflow between specialized agents"),
    SEARCH("Performs web search and vector retrieval operations"),
    ANALYSIS("Processes, summarizes, and evaluates data"),
    KNOWLEDGE("Queries knowledge base documents and structured RAG context");

    private final String description;

    AgentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
