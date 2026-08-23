package com.ainexus.service.impl;

import com.ainexus.dto.WorkflowRequest;
import com.ainexus.dto.WorkflowResponse;
import com.ainexus.dto.WorkflowStepRequest;
import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkflowRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    private final WorkflowRepository workflowRepository;
    private final WorkspaceRepository workspaceRepository;

    @Value("${app.workflow.max-steps:20}")
    private int maxSteps = 20;

    public WorkflowServiceImpl(WorkflowRepository workflowRepository, WorkspaceRepository workspaceRepository) {
        this.workflowRepository = workflowRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    @Override
    public WorkflowResponse createWorkflow(WorkflowRequest request, User user) {
        Objects.requireNonNull(request, "WorkflowRequest must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workspace workspace = authorizeWorkspaceAccess(request.workspaceId(), user);

        if (workflowRepository.existsByWorkspaceIdAndNameIgnoreCase(request.workspaceId(), request.name().trim())) {
            throw new IllegalArgumentException("A workflow with the name '" + request.name().trim() + "' already exists in this workspace.");
        }

        validateWorkflowSteps(request.steps());

        Workflow workflow = new Workflow(request.name().trim(), request.description(), workspace, user);
        if (request.status() != null) {
            workflow.setStatus(request.status());
        }

        if (request.steps() != null) {
            int order = 1;
            for (WorkflowStepRequest stepReq : request.steps()) {
                WorkflowStep step = new WorkflowStep(
                        stepReq.stepKey().trim(),
                        stepReq.name().trim(),
                        stepReq.type(),
                        stepReq.configuration(),
                        stepReq.executionOrder() != null ? stepReq.executionOrder() : order++,
                        stepReq.dependencies(),
                        stepReq.enabled() == null || stepReq.enabled()
                );
                workflow.addStep(step);
            }
        }

        Workflow saved = workflowRepository.save(workflow);
        logger.info("Created workflow id: {} '{}' in workspace id: {} by user: {}", saved.getId(), saved.getName(), workspace.getId(), user.getUsername());
        return WorkflowResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(Long workflowId, User user) {
        Objects.requireNonNull(workflowId, "Workflow ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workflow workflow = workflowRepository.findByIdWithSteps(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        authorizeWorkspaceAccess(workflow.getWorkspace().getId(), user);
        return WorkflowResponse.fromEntity(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowResponse> getWorkflowsByWorkspace(Long workspaceId, User user) {
        Objects.requireNonNull(workspaceId, "Workspace ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        authorizeWorkspaceAccess(workspaceId, user);
        List<Workflow> workflows = workflowRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        return workflows.stream().map(WorkflowResponse::fromEntity).toList();
    }

    @Override
    public WorkflowResponse updateWorkflow(Long workflowId, WorkflowRequest request, User user) {
        Objects.requireNonNull(workflowId, "Workflow ID must not be null");
        Objects.requireNonNull(request, "WorkflowRequest must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workflow workflow = workflowRepository.findByIdWithSteps(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        authorizeWorkspaceAccess(workflow.getWorkspace().getId(), user);

        if (!workflow.getName().equalsIgnoreCase(request.name().trim()) &&
                workflowRepository.existsByWorkspaceIdAndNameIgnoreCase(workflow.getWorkspace().getId(), request.name().trim())) {
            throw new IllegalArgumentException("A workflow with the name '" + request.name().trim() + "' already exists in this workspace.");
        }

        validateWorkflowSteps(request.steps());

        workflow.setName(request.name().trim());
        workflow.setDescription(request.description());
        if (request.status() != null) {
            workflow.setStatus(request.status());
        }
        workflow.setVersion(workflow.getVersion() + 1);

        workflow.clearSteps();
        if (request.steps() != null) {
            int order = 1;
            for (WorkflowStepRequest stepReq : request.steps()) {
                WorkflowStep step = new WorkflowStep(
                        stepReq.stepKey().trim(),
                        stepReq.name().trim(),
                        stepReq.type(),
                        stepReq.configuration(),
                        stepReq.executionOrder() != null ? stepReq.executionOrder() : order++,
                        stepReq.dependencies(),
                        stepReq.enabled() == null || stepReq.enabled()
                );
                workflow.addStep(step);
            }
        }

        Workflow updated = workflowRepository.save(workflow);
        logger.info("Updated workflow id: {} to version: {} by user: {}", updated.getId(), updated.getVersion(), user.getUsername());
        return WorkflowResponse.fromEntity(updated);
    }

    @Override
    public void deleteWorkflow(Long workflowId, User user) {
        Objects.requireNonNull(workflowId, "Workflow ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        authorizeWorkspaceAccess(workflow.getWorkspace().getId(), user);

        workflowRepository.delete(workflow);
        logger.info("Deleted workflow id: {} by user: {}", workflowId, user.getUsername());
    }

    @Override
    public WorkflowResponse updateWorkflowStatus(Long workflowId, WorkflowStatus status, User user) {
        Objects.requireNonNull(workflowId, "Workflow ID must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workflow workflow = workflowRepository.findByIdWithSteps(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        authorizeWorkspaceAccess(workflow.getWorkspace().getId(), user);

        workflow.setStatus(status);
        Workflow saved = workflowRepository.save(workflow);
        return WorkflowResponse.fromEntity(saved);
    }

    private Workspace authorizeWorkspaceAccess(Long workspaceId, User user) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));

        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
        if (!isOwner) {
            logger.warn("User {} is unauthorized for workspace {}", user.getUsername(), workspaceId);
            throw new UnauthorizedAccessException("You are not authorized to access workspace ID: " + workspaceId);
        }
        return workspace;
    }

    private void validateWorkflowSteps(List<WorkflowStepRequest> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }

        if (steps.size() > maxSteps) {
            throw new IllegalArgumentException("Workflow steps count (" + steps.size() + ") exceeds the maximum allowed limit of " + maxSteps);
        }

        Set<String> stepKeys = new HashSet<>();
        Map<String, List<String>> dependencyGraph = new HashMap<>();

        for (WorkflowStepRequest step : steps) {
            if (step.stepKey() == null || step.stepKey().trim().isBlank()) {
                throw new IllegalArgumentException("Step key cannot be blank.");
            }
            String key = step.stepKey().trim();
            if (stepKeys.contains(key)) {
                throw new IllegalArgumentException("Duplicate step key detected: '" + key + "'");
            }
            stepKeys.add(key);
            dependencyGraph.put(key, step.dependencies() != null ? step.dependencies() : List.of());
        }

        // Validate dependency keys exist and no self-dependency
        for (Map.Entry<String, List<String>> entry : dependencyGraph.entrySet()) {
            String stepKey = entry.getKey();
            for (String depKey : entry.getValue()) {
                if (depKey == null || depKey.trim().isBlank()) continue;
                String cleanDep = depKey.trim();
                if (cleanDep.equals(stepKey)) {
                    throw new IllegalArgumentException("Step '" + stepKey + "' cannot depend on itself.");
                }
                if (!stepKeys.contains(cleanDep)) {
                    throw new IllegalArgumentException("Step '" + stepKey + "' references non-existent dependency: '" + cleanDep + "'");
                }
            }
        }

        // Detect circular dependencies (DAG cycle detection)
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String key : stepKeys) {
            if (hasCycle(key, dependencyGraph, visited, recursionStack)) {
                throw new IllegalArgumentException("Circular dependency detected in workflow steps involving step: '" + key + "'");
            }
        }
    }

    private boolean hasCycle(String current, Map<String, List<String>> graph, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(current)) {
            return true;
        }
        if (visited.contains(current)) {
            return false;
        }

        visited.add(current);
        recursionStack.add(current);

        List<String> deps = graph.getOrDefault(current, List.of());
        for (String dep : deps) {
            if (dep != null && !dep.trim().isBlank()) {
                if (hasCycle(dep.trim(), graph, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(current);
        return false;
    }
}
