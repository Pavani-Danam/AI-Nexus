package com.ainexus.controller;

import com.ainexus.dto.UpdateWorkspaceQuotaRequest;
import com.ainexus.dto.WorkspaceQuotaResponse;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.UserRepository;
import com.ainexus.service.UsageQuotaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/quotas")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'ROLE_ADMIN')")
public class AdminQuotaController {

    private final UsageQuotaService quotaService;
    private final UserRepository userRepository;

    public AdminQuotaController(UsageQuotaService quotaService, UserRepository userRepository) {
        this.quotaService = quotaService;
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
    public ResponseEntity<List<WorkspaceQuotaResponse>> listAllQuotas(Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(quotaService.listAllWorkspaceQuotas(admin));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceQuotaResponse> getWorkspaceQuota(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(quotaService.getWorkspaceQuota(workspaceId, admin));
    }

    @PutMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceQuotaResponse> updateWorkspaceQuota(
            @PathVariable Long workspaceId,
            @Valid @RequestBody UpdateWorkspaceQuotaRequest request,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(quotaService.updateWorkspaceQuota(workspaceId, request, admin));
    }

    @PostMapping("/{workspaceId}/reset")
    public ResponseEntity<WorkspaceQuotaResponse> resetWorkspaceUsage(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(quotaService.resetWorkspaceUsage(workspaceId, admin));
    }
}
