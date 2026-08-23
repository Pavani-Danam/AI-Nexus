package com.ainexus.controller;

import com.ainexus.dto.WorkflowScheduleRequest;
import com.ainexus.dto.WorkflowScheduleResponse;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.UserRepository;
import com.ainexus.service.WorkflowScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WorkflowScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowScheduleController.class);

    private final WorkflowScheduleService workflowScheduleService;
    private final UserRepository userRepository;

    public WorkflowScheduleController(
            WorkflowScheduleService workflowScheduleService,
            UserRepository userRepository) {
        this.workflowScheduleService = workflowScheduleService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authentication principal is missing.");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for username: " + authentication.getName()));
    }

    @PostMapping("/workflows/{workflowId}/schedules")
    public ResponseEntity<WorkflowScheduleResponse> createSchedule(
            @PathVariable Long workflowId,
            @RequestBody WorkflowScheduleRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        logger.info("REST: Creating schedule for workflow id: {} by user: {}", workflowId, user.getUsername());
        WorkflowScheduleResponse response = workflowScheduleService.createSchedule(workflowId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/schedules/{scheduleId}")
    public ResponseEntity<WorkflowScheduleResponse> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody WorkflowScheduleRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        WorkflowScheduleResponse response = workflowScheduleService.updateSchedule(scheduleId, request, user);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/schedules/{scheduleId}/toggle")
    public ResponseEntity<WorkflowScheduleResponse> toggleSchedule(
            @PathVariable Long scheduleId,
            @RequestParam boolean enabled,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        WorkflowScheduleResponse response = workflowScheduleService.toggleSchedule(scheduleId, enabled, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/schedules/{scheduleId}")
    public ResponseEntity<WorkflowScheduleResponse> getScheduleById(
            @PathVariable Long scheduleId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        WorkflowScheduleResponse response = workflowScheduleService.getScheduleById(scheduleId, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workflows/{workflowId}/schedules")
    public ResponseEntity<List<WorkflowScheduleResponse>> getSchedulesByWorkflow(
            @PathVariable Long workflowId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<WorkflowScheduleResponse> responses = workflowScheduleService.getSchedulesByWorkflow(workflowId, user);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/workspaces/{workspaceId}/schedules")
    public ResponseEntity<List<WorkflowScheduleResponse>> getSchedulesByWorkspace(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<WorkflowScheduleResponse> responses = workflowScheduleService.getSchedulesByWorkspace(workspaceId, user);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long scheduleId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        workflowScheduleService.deleteSchedule(scheduleId, user);
        return ResponseEntity.noContent().build();
    }
}
