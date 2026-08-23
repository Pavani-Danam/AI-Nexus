package com.ainexus.controller;

import com.ainexus.dto.AdminUserResponse;
import com.ainexus.dto.ManageWorkspaceMembershipRequest;
import com.ainexus.dto.UpdateUserRoleRequest;
import com.ainexus.dto.UpdateUserStatusRequest;
import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.UserRepository;
import com.ainexus.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'ROLE_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserRepository userRepository;

    public AdminUserController(AdminUserService adminUserService, UserRepository userRepository) {
        this.adminUserService = adminUserService;
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
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminUserService.listUsers(search, pageable, admin));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserResponse> getUserDetails(
            @PathVariable Long userId,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(adminUserService.getUserDetails(userId, admin));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(adminUserService.updateUserStatus(userId, request, admin));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<AdminUserResponse> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(adminUserService.updateUserRole(userId, request, admin));
    }

    @PostMapping("/{userId}/workspaces")
    public ResponseEntity<AdminUserResponse> assignWorkspaceMembership(
            @PathVariable Long userId,
            @Valid @RequestBody ManageWorkspaceMembershipRequest request,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(adminUserService.assignWorkspaceMembership(userId, request, admin));
    }

    @DeleteMapping("/{userId}/workspaces/{workspaceId}")
    public ResponseEntity<AdminUserResponse> removeWorkspaceMembership(
            @PathVariable Long userId,
            @PathVariable Long workspaceId,
            Authentication authentication) {
        User admin = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(adminUserService.removeWorkspaceMembership(userId, workspaceId, admin));
    }
}
