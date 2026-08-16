package com.ainexus.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class AgentOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final Map<AgentType, Agent> agentRegistry = new EnumMap<>(AgentType.class);

    private static final Pattern SEARCH_PATTERN = Pattern.compile(
            "\\b(find|search|lookup|look for|retrieve|locate|fetch|query|list documents)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ANALYSIS_PATTERN = Pattern.compile(
            "\\b(compare|contrast|difference|similarities|summarize|summary|evaluate|analyze|break down)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public AgentOrchestrator(List<Agent> agents) {
        if (agents != null) {
            for (Agent agent : agents) {
                if (agent != null && agent.getAgentType() != null) {
                    agentRegistry.put(agent.getAgentType(), agent);
                    logger.info("Registered agent: {} ({})", agent.getAgentType(), agent.getClass().getSimpleName());
                }
            }
        }
    }

    public AgentResult orchestrate(AgentRequest request) {
        Objects.requireNonNull(request, "AgentRequest must not be null");

        String traceId = (request.traceId() != null && !request.traceId().isBlank())
                ? request.traceId()
                : UUID.randomUUID().toString();

        logger.info("[Trace: {}] Orchestrator received request for workspace id: {} (target: {})",
                traceId, request.workspaceId(), request.targetAgent());

        // Determine destination agent type
        AgentType destinationType = resolveAgentType(request);

        Agent targetAgent = agentRegistry.get(destinationType);
        if (targetAgent == null) {
            logger.error("[Trace: {}] No registered agent available for type: {}", traceId, destinationType);
            throw new AgentException("No agent implementation available for type: " + destinationType, destinationType, traceId);
        }

        // Initialize shared execution context
        AgentContext context = new AgentContext(traceId, request.workspaceId(), request.user());
        context.setMetadata("routedAgent", destinationType.name());

        logger.info("[Trace: {}] Routing query '{}' to agent: {}", traceId, request.query(), destinationType);

        try {
            AgentResult result = targetAgent.execute(request, context);
            logger.info("[Trace: {}] Agent {} completed execution successfully", traceId, destinationType);
            return result;
        } catch (AgentException e) {
            logger.error("[Trace: {}] Agent {} execution error: {}", traceId, destinationType, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("[Trace: {}] Unexpected failure executing agent {}: {}", traceId, destinationType, e.getMessage());
            throw new AgentException("Agent execution failed: " + e.getMessage(), destinationType, traceId, e);
        }
    }

    public AgentType resolveAgentType(AgentRequest request) {
        if (request.targetAgent() != null && request.targetAgent() != AgentType.ORCHESTRATOR) {
            return request.targetAgent();
        }

        String query = request.query() != null ? request.query().trim() : "";

        if (ANALYSIS_PATTERN.matcher(query).find()) {
            return AgentType.ANALYSIS;
        }

        if (SEARCH_PATTERN.matcher(query).find()) {
            return AgentType.SEARCH;
        }

        // Default knowledge answer routing
        return AgentType.KNOWLEDGE;
    }

    public boolean hasAgent(AgentType type) {
        return agentRegistry.containsKey(type);
    }
}
