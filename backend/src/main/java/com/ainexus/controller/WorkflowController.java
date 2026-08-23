package com.ainexus.controller;

import com.ainexus.dto.WorkflowExecutionRequest;
import com.ainexus.dto.WorkflowExecutionResponse;
import com.ainexus.dto.WorkflowRequest;
import com.ainexus.dto.WorkflowResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowStatus;
import com.ainexus.service.WorkflowExecutionService;
import com.ainexus.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowExecutionService workflowExecutionService;

    public WorkflowController(WorkflowService workflowService, WorkflowExecutionService workflowExecutionService) {
        this.workflowService = workflowService;
        this.workflowExecutionService = workflowExecutionService;
    }

    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @Valid @RequestBody WorkflowRequest request,
            @AuthenticationPrincipal User user) {
        WorkflowResponse response = workflowService.createWorkflow(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflowById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        WorkflowResponse response = workflowService.getWorkflowById(id, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> getWorkflowsByWorkspace(
            @RequestParam Long workspaceId,
            @AuthenticationPrincipal User user) {
        List<WorkflowResponse> responses = workflowService.getWorkflowsByWorkspace(workspaceId, user);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowRequest request,
            @AuthenticationPrincipal User user) {
        WorkflowResponse response = workflowService.updateWorkflow(id, request, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        workflowService.deleteWorkflow(id, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WorkflowResponse> updateWorkflowStatus(
            @PathVariable Long id,
            @RequestParam WorkflowStatus status,
            @AuthenticationPrincipal User user) {
        WorkflowResponse response = workflowService.updateWorkflowStatus(id, status, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<WorkflowExecutionResponse> executeWorkflow(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowExecutionRequest request,
            @AuthenticationPrincipal User user) {
        WorkflowExecutionResponse response = workflowExecutionService.executeWorkflow(id, request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<WorkflowExecutionResponse> getExecutionById(
            @PathVariable Long executionId,
            @AuthenticationPrincipal User user) {
        WorkflowExecutionResponse response = workflowExecutionService.getExecutionById(executionId, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<WorkflowExecutionResponse>> getExecutionsByWorkflow(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        List<WorkflowExecutionResponse> responses = workflowExecutionService.getExecutionsByWorkflow(id, user);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/executions/workspace/{workspaceId}")
    public ResponseEntity<List<WorkflowExecutionResponse>> getExecutionsByWorkspace(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal User user) {
        List<WorkflowExecutionResponse> responses = workflowExecutionService.getExecutionsByWorkspace(workspaceId, user);
        return ResponseEntity.ok(responses);
    }
}
