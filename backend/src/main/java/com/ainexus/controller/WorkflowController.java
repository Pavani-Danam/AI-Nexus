package com.ainexus.controller;

import com.ainexus.dto.WorkflowRequest;
import com.ainexus.dto.WorkflowResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowStatus;
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

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
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
}
