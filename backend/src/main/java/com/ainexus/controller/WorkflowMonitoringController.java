package com.ainexus.controller;

import com.ainexus.dto.WorkflowExecutionResponse;
import com.ainexus.dto.WorkflowMonitoringSummaryResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowAuditEvent;
import com.ainexus.entity.WorkflowExecutionStatus;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.UserRepository;
import com.ainexus.service.WorkflowMonitoringService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WorkflowMonitoringController {

    private final WorkflowMonitoringService monitoringService;
    private final UserRepository userRepository;

    public WorkflowMonitoringController(WorkflowMonitoringService monitoringService, UserRepository userRepository) {
        this.monitoringService = monitoringService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authentication principal is missing.");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    @GetMapping("/workspaces/{workspaceId}/workflow-monitoring/summary")
    public ResponseEntity<WorkflowMonitoringSummaryResponse> getWorkspaceSummary(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(monitoringService.getWorkspaceMonitoringSummary(workspaceId, user));
    }

    @GetMapping("/workspaces/{workspaceId}/workflow-executions")
    public ResponseEntity<Page<WorkflowExecutionResponse>> getExecutionHistory(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) Long workflowId,
            @RequestParam(required = false) WorkflowExecutionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(monitoringService.getExecutionHistory(workspaceId, workflowId, status, pageable, user));
    }

    @GetMapping("/workflow-executions/{executionId}/details")
    public ResponseEntity<WorkflowExecutionResponse> getExecutionDetails(
            @PathVariable Long executionId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(monitoringService.getExecutionDetails(executionId, user));
    }

    @GetMapping("/workflow-executions/{executionId}/audit-trail")
    public ResponseEntity<List<WorkflowAuditEvent>> getExecutionAuditTrail(
            @PathVariable Long executionId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(monitoringService.getAuditEventsByExecution(executionId, user));
    }

    @GetMapping("/workspaces/{workspaceId}/workflow-audit-events")
    public ResponseEntity<Page<WorkflowAuditEvent>> getWorkspaceAuditEvents(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(monitoringService.getAuditEventsByWorkspace(workspaceId, pageable, user));
    }
}
