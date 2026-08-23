package com.ainexus.controller;

import com.ainexus.dto.WorkflowExecutionResponse;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.UserRepository;
import com.ainexus.service.WorkflowRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WorkflowRecoveryController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowRecoveryController.class);

    private final WorkflowRecoveryService workflowRecoveryService;
    private final UserRepository userRepository;

    public WorkflowRecoveryController(
            WorkflowRecoveryService workflowRecoveryService,
            UserRepository userRepository) {
        this.workflowRecoveryService = workflowRecoveryService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authentication principal is missing.");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for username: " + authentication.getName()));
    }

    @PostMapping("/workflow-executions/{executionId}/recover")
    public ResponseEntity<WorkflowExecutionResponse> recoverExecution(
            @PathVariable Long executionId,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        logger.info("REST: Triggering failure recovery for execution id: {} by user: {}", executionId, user.getUsername());
        WorkflowExecutionResponse response = workflowRecoveryService.recoverExecution(executionId, user);
        return ResponseEntity.ok(response);
    }
}
