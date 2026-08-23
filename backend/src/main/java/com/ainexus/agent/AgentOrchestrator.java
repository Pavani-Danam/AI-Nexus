package com.ainexus.agent;

import com.ainexus.dto.*;
import com.ainexus.service.AgentPlanningService;
import com.ainexus.service.PlanExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class AgentOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final Map<AgentType, Agent> agentRegistry = new EnumMap<>(AgentType.class);
    private final AgentPlanningService planningService;
    private final PlanExecutionService planExecutionService;

    private static final Pattern SEARCH_PATTERN = Pattern.compile(
            "\\b(find|search|lookup|look for|retrieve|locate|fetch|query|list documents)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ANALYSIS_PATTERN = Pattern.compile(
            "\\b(compare|contrast|difference|similarities|summarize|summary|evaluate|analyze|break down)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public AgentOrchestrator(List<Agent> agents) {
        this(agents, null, null);
    }

    @Autowired
    public AgentOrchestrator(
            List<Agent> agents,
            @Autowired(required = false) AgentPlanningService planningService,
            @Autowired(required = false) PlanExecutionService planExecutionService) {
        this.planningService = planningService;
        this.planExecutionService = planExecutionService;

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
        return orchestrate(request, null);
    }

    public AgentResult orchestrate(AgentRequest request, ConversationMemory memory) {
        Objects.requireNonNull(request, "AgentRequest must not be null");

        String traceId = (request.traceId() != null && !request.traceId().isBlank())
                ? request.traceId()
                : UUID.randomUUID().toString();

        logger.info("[Trace: {}] Orchestrator received request for workspace id: {} (target: {})",
                traceId, request.workspaceId(), request.targetAgent());

        // If target agent is ORCHESTRATOR or not specifically locked to a specialized leaf agent, use autonomous planning execution
        if ((request.targetAgent() == null || request.targetAgent() == AgentType.ORCHESTRATOR) && planningService != null && planExecutionService != null) {
            return executeAutonomous(request, memory, traceId);
        }

        // Direct single-agent routing fallback
        AgentType destinationType = resolveAgentType(request);
        Agent targetAgent = agentRegistry.get(destinationType);
        if (targetAgent == null) {
            logger.error("[Trace: {}] No registered agent available for type: {}", traceId, destinationType);
            throw new AgentException("No agent implementation available for type: " + destinationType, destinationType, traceId);
        }

        AgentContext context = new AgentContext(traceId, request.workspaceId(), request.user());
        context.setMetadata("routedAgent", destinationType.name());

        logger.info("[Trace: {}] Routing query '{}' directly to agent: {}", traceId, request.query(), destinationType);

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

    public AgentResult executeAutonomous(AgentRequest request, ConversationMemory memory, String traceId) {
        logger.info("[Trace: {}] Starting autonomous agent planning & execution for query: '{}'", traceId, request.query());
        long startTime = System.currentTimeMillis();

        try {
            // 1. Create plan
            AgentPlan plan = planningService.createPlan(request.query(), request.workspaceId(), memory, request.user());
            logger.info("[Trace: {}] Autonomous plan generated: {} with {} tasks", traceId, plan.planId(), plan.tasks().size());

            // 2. Execute plan (includes DAG resolution, parallel tasks, retries & bounded replanning)
            AgentExecutionResult execResult = planExecutionService.executePlan(plan, memory, request.user());
            long duration = System.currentTimeMillis() - startTime;

            logger.info("[Trace: {}] Autonomous execution completed with status {} in {}ms", traceId, execResult.status(), duration);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("planId", execResult.planId());
            metadata.put("executionId", execResult.executionId());
            metadata.put("executionStatus", execResult.status().name());
            metadata.put("durationMs", duration);
            metadata.put("taskCount", execResult.taskResults().size());

            return AgentResult.success(
                    AgentType.ORCHESTRATOR,
                    traceId,
                    execResult.finalOutput(),
                    List.of(),
                    metadata
            );
        } catch (AgentException ae) {
            throw ae;
        } catch (Throwable t) {
            logger.error("[Trace: {}] Autonomous agent execution failed: {}", traceId, t.getMessage());
            throw new AgentException("Autonomous execution failure: " + t.getMessage(), AgentType.ORCHESTRATOR, traceId, t);
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

        return AgentType.KNOWLEDGE;
    }

    public boolean hasAgent(AgentType type) {
        return agentRegistry.containsKey(type);
    }
}
