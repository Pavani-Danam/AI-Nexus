package com.ainexus.controller;

import com.ainexus.dto.WorkflowApprovalDecisionRequest;
import com.ainexus.dto.WorkflowApprovalResponse;
import com.ainexus.dto.WorkflowExecutionResponse;
import com.ainexus.entity.User;
import com.ainexus.service.UserService;
import com.ainexus.service.WorkflowApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WorkflowApprovalController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowApprovalController.class);

    private final WorkflowApprovalService workflowApprovalService;
    private final UserService userService;

    public WorkflowApprovalController(
            WorkflowApprovalService workflowApprovalService,
            UserService userService) {
        this.workflowApprovalService = workflowApprovalService;
        this.userService = userService;
    }

    @GetMapping("/approvals/{approvalId}")
    public ResponseEntity<WorkflowApprovalResponse> getApprovalById(
            @PathVariable Long approvalId,
            Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        WorkflowApprovalResponse response = workflowApprovalService.getApprovalById(approvalId, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workspaces/{workspaceId}/approvals/pending")
    public ResponseEntity<List<WorkflowApprovalResponse>> getPendingApprovals(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        List<WorkflowApprovalResponse> responses = workflowApprovalService.getPendingApprovalsByWorkspace(workspaceId, user);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/workflow-executions/{executionId}/approvals")
    public ResponseEntity<List<WorkflowApprovalResponse>> getApprovalsByExecution(
            @PathVariable Long executionId,
            Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        List<WorkflowApprovalResponse> responses = workflowApprovalService.getApprovalsByExecution(executionId, user);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/approvals/{approvalId}/approve")
    public ResponseEntity<WorkflowExecutionResponse> approveStep(
            @PathVariable Long approvalId,
            @RequestBody(required = false) WorkflowApprovalDecisionRequest request,
            Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        logger.info("REST: User {} approving gate id: {}", user.getUsername(), approvalId);
        WorkflowExecutionResponse response = workflowApprovalService.approveStep(approvalId, request, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approvals/{approvalId}/reject")
    public ResponseEntity<WorkflowExecutionResponse> rejectStep(
            @PathVariable Long approvalId,
            @RequestBody(required = false) WorkflowApprovalDecisionRequest request,
            Authentication authentication) {
        User user = userService.getUserFromAuthentication(authentication);
        logger.info("REST: User {} rejecting gate id: {}", user.getUsername(), approvalId);
        WorkflowExecutionResponse response = workflowApprovalService.rejectStep(approvalId, request, user);
        return ResponseEntity.ok(response);
    }
}
