package com.ainexus.controller;

import com.ainexus.dto.AdminCreateWorkspaceRequest;
import com.ainexus.dto.AdminUpdateWorkspaceRequest;
import com.ainexus.dto.AdminWorkspaceDetailResponse;
import com.ainexus.dto.ManageWorkspaceMembershipRequest;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.UserRepository;
import com.ainexus.service.AdminWorkspaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/workspaces")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'ROLE_ADMIN')")
public class AdminWorkspaceController {

    private final AdminWorkspaceService adminWorkspaceService;
    private final UserRepository userRepository;

    public AdminWorkspaceController(AdminWorkspaceService adminWorkspaceService, UserRepository userRepository) {
        this.adminWorkspaceService = adminWorkspaceService;
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
    public ResponseEntity<Page<AdminWorkspaceDetailResponse>> listWorkspaces(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminWorkspaceService.listWorkspaces(search, pageable, admin));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<AdminWorkspaceDetailResponse> getWorkspaceDetails(
            @PathVariable Long workspaceId,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(adminWorkspaceService.getWorkspaceDetails(workspaceId, admin));
    }

    @PostMapping
    public ResponseEntity<AdminWorkspaceDetailResponse> createWorkspace(
            @Valid @RequestBody AdminCreateWorkspaceRequest request,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(adminWorkspaceService.createWorkspace(request, admin));
    }

    @PutMapping("/{workspaceId}")
    public ResponseEntity<AdminWorkspaceDetailResponse> updateWorkspace(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AdminUpdateWorkspaceRequest request,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(adminWorkspaceService.updateWorkspace(workspaceId, request, admin));
    }

    @PostMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<AdminWorkspaceDetailResponse> addOrUpdateMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            @Valid @RequestBody ManageWorkspaceMembershipRequest request,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(adminWorkspaceService.addOrUpdateMember(workspaceId, request, userId, admin));
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<AdminWorkspaceDetailResponse> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(adminWorkspaceService.removeMember(workspaceId, userId, admin));
    }
}
