package com.ainexus.controller;

import com.ainexus.dto.AuditLogResponse;
import com.ainexus.dto.AuditLogSearchRequest;
import com.ainexus.entity.AuditActionType;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.UserRepository;
import com.ainexus.service.EnterpriseAuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit-logs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class EnterpriseAuditController {

    private final EnterpriseAuditService auditService;
    private final UserRepository userRepository;

    public EnterpriseAuditController(EnterpriseAuditService auditService, UserRepository userRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authentication principal missing.");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> searchAuditLogs(
            @RequestParam(required = false) Long workspaceId,
            @RequestParam(required = false) AuditActionType actionType,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User requester = getAuthenticatedUser(authentication);
        AuditLogSearchRequest filter = new AuditLogSearchRequest(workspaceId, actionType, actorUsername, startDate, endDate);
        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(auditService.searchAuditLogs(filter, pageable, requester));
    }

    @GetMapping("/{auditId}")
    public ResponseEntity<AuditLogResponse> getAuditLogDetails(
            @PathVariable Long auditId,
            Authentication authentication) {

        User requester = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(auditService.getAuditLogDetails(auditId, requester));
    }
}
