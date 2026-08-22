package com.ainexus.service.impl;

import com.ainexus.dto.*;
import com.ainexus.entity.User;
import com.ainexus.service.AgentPlanningService;
import com.ainexus.service.AgentReplanningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentReplanningServiceImpl implements AgentReplanningService {

    private static final Logger logger = LoggerFactory.getLogger(AgentReplanningServiceImpl.class);

    private final AgentPlanningService planningService;

    @Value("${app.agent.replanning.max-attempts:2}")
    private int maxReplanningAttempts = 2;

    @Autowired
    public AgentReplanningServiceImpl(AgentPlanningService planningService) {
        this.planningService = planningService;
    }

    public void setMaxReplanningAttempts(int maxReplanningAttempts) {
        this.maxReplanningAttempts = maxReplanningAttempts;
    }

    public int getMaxReplanningAttempts() {
        return maxReplanningAttempts;
    }

    @Override
    public boolean shouldReplan(AgentPlan currentPlan, AgentExecutionResult executionResult) {
        if (currentPlan == null || executionResult == null) {
            return false;
        }

        // 1. If execution was completely successful and produced substantive output, no replan needed
        if (executionResult.status() == PlanExecutionStatus.COMPLETED) {
            String output = executionResult.finalOutput();
            if (output != null && !isInsufficientOutput(output)) {
                return false;
            }
            logger.info("Plan {} completed but output was deemed insufficient. Triggering replan evaluation.", currentPlan.planId());
            return true;
        }

        // 2. Check failed tasks for non-recoverable categories
        for (AgentTaskResult taskResult : executionResult.taskResults()) {
            if (taskResult.status() == AgentTaskStatus.FAILED) {
                FailureCategory category = taskResult.failureCategory();
                if (category == FailureCategory.AUTHORIZATION_FAILURE || category == FailureCategory.VALIDATION_FAILURE) {
                    logger.warn("Task {} failed with non-recoverable category {}. Skipping replanning.", taskResult.taskId(), category);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isInsufficientOutput(String output) {
        if (output == null || output.trim().isEmpty()) {
            return true;
        }
        String lower = output.toLowerCase().trim();
        return lower.contains("no relevant information") ||
               lower.contains("no documents found") ||
               lower.contains("empty answer") ||
               lower.contains("with no results") ||
               lower.contains("with no findings") ||
               lower.equals("no task outputs produced during plan execution.");
    }

    @Override
    public AgentPlan generateCorrectedPlan(
            AgentPlan failedPlan,
            AgentExecutionResult executionResult,
            int replanAttempt,
            ConversationMemory memory,
            User authenticatedUser) {

        if (failedPlan == null) {
            throw new IllegalArgumentException("Failed plan cannot be null for replanning.");
        }
        if (replanAttempt > maxReplanningAttempts) {
            logger.warn("Replan attempt {} exceeds configured max limit {}. Stopping replanning.", replanAttempt, maxReplanningAttempts);
            return null;
        }

        String basePlanId = failedPlan.planId();
        if (basePlanId.contains("-v")) {
            basePlanId = basePlanId.replaceAll("-v\\d+", "");
        }
        String newPlanId = basePlanId + "-v" + (replanAttempt + 1);

        logger.info("Generating corrected plan {} (attempt {}/{}) for original query: '{}'",
                newPlanId, replanAttempt, maxReplanningAttempts, failedPlan.originalQuery());

        List<AgentTask> correctedTasks = new ArrayList<>();
        Map<String, String> outputs = executionResult != null ? executionResult.outputsByTaskId() : Map.of();

        // Reformulate tasks: refine search terms for tasks that returned empty or failed
        for (AgentTask originalTask : failedPlan.tasks()) {
            String currentOutput = outputs.get(originalTask.id());
            boolean taskNeedsReformulation = currentOutput == null || isInsufficientOutput(currentOutput);

            if (originalTask.type() == AgentTaskType.SEARCH && taskNeedsReformulation) {
                String refinedDescription = "Search related keywords, synonyms, and context for: " + originalTask.description();
                correctedTasks.add(new AgentTask(originalTask.id(), originalTask.type(), refinedDescription, originalTask.dependsOn()));
            } else {
                correctedTasks.add(originalTask);
            }
        }

        // If tasks didn't change, expand search coverage with a dedicated secondary search task
        if (correctedTasks.equals(failedPlan.tasks())) {
            String extraTaskId = "task-" + (correctedTasks.size() + 1);
            correctedTasks.add(new AgentTask(
                    extraTaskId,
                    AgentTaskType.SEARCH,
                    "Broad workspace keyword search for: " + failedPlan.originalQuery(),
                    List.of()
            ));
        }

        AgentPlan correctedPlan = new AgentPlan(
                newPlanId,
                failedPlan.originalQuery(), // Preserve exact original query
                failedPlan.workspaceId(),
                correctedTasks,
                "Replanned correction version " + (replanAttempt + 1),
                correctedTasks.size() > 1
        );

        if (planningService != null && !planningService.validatePlan(correctedPlan)) {
            logger.error("Corrected plan {} failed DAG validation.", newPlanId);
            return null;
        }

        return correctedPlan;
    }
}
