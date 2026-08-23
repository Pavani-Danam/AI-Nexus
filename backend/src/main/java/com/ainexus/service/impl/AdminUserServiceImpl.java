package com.ainexus.service.impl;

import com.ainexus.dto.AdminUserResponse;
import com.ainexus.dto.ManageWorkspaceMembershipRequest;
import com.ainexus.dto.UpdateUserRoleRequest;
import com.ainexus.dto.UpdateUserStatusRequest;
import com.ainexus.dto.UserWorkspaceMembershipDto;
import com.ainexus.entity.Role;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.entity.WorkspaceMember;
import com.ainexus.entity.WorkflowAuditEventType;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.UserRepository;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.AdminUserService;
import com.ainexus.service.WorkflowMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkflowMonitoringService monitoringService;

    public AdminUserServiceImpl(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkflowMonitoringService monitoringService) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.monitoringService = monitoringService;
    }

    private void checkAdminAccess(User user) {
        Objects.requireNonNull(user, "User must not be null");
        String roleStr = user.getRole() != null ? user.getRole().name() : "";
        if (!roleStr.contains("ADMIN")) {
            logger.warn("[SECURITY] User '{}' attempted admin operation without ROLE_ADMIN", user.getUsername());
            throw new UnauthorizedAccessException("Forbidden: Administrator privileges required.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String query, Pageable pageable, User adminUser) {
        checkAdminAccess(adminUser);

        List<User> allUsers = userRepository.findAll();
        List<User> filtered = allUsers.stream()
                .filter(u -> {
                    if (query == null || query.isBlank()) return true;
                    String q = query.toLowerCase().trim();
                    boolean matchUsername = u.getUsername() != null && u.getUsername().toLowerCase().contains(q);
                    boolean matchEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(q);
                    boolean matchName = u.getName() != null && u.getName().toLowerCase().contains(q);
                    return matchUsername || matchEmail || matchName;
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<AdminUserResponse> content = (start <= end && start < filtered.size())
                ? filtered.subList(start, end).stream().map(this::buildUserResponse).toList()
                : List.of();

        return new PageImpl<>(content, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserDetails(Long userId, User adminUser) {
        checkAdminAccess(adminUser);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return buildUserResponse(user);
    }

    @Override
    public AdminUserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request, User adminUser) {
        checkAdminAccess(adminUser);
        Objects.requireNonNull(request, "Request must not be null");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getId().equals(adminUser.getId()) && !request.enabled()) {
            throw new IllegalArgumentException("Admins cannot deactivate their own account.");
        }

        user.setEnabled(request.enabled());
        User updated = userRepository.save(user);

        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_UPDATED,
                null,
                1L,
                null,
                adminUser.getUsername(),
                "User account status changed for " + user.getUsername() + " to enabled=" + request.enabled()
        );

        return buildUserResponse(updated);
    }

    @Override
    public AdminUserResponse updateUserRole(Long userId, UpdateUserRoleRequest request, User adminUser) {
        checkAdminAccess(adminUser);
        Objects.requireNonNull(request, "Request must not be null");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getId().equals(adminUser.getId()) && request.role() != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Admins cannot revoke their own administrator role.");
        }

        user.setRole(request.role());
        User updated = userRepository.save(user);

        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_UPDATED,
                null,
                1L,
                null,
                adminUser.getUsername(),
                "User role updated for " + user.getUsername() + " to " + request.role()
        );

        return buildUserResponse(updated);
    }

    @Override
    public AdminUserResponse assignWorkspaceMembership(Long userId, ManageWorkspaceMembershipRequest request, User adminUser) {
        checkAdminAccess(adminUser);
        Objects.requireNonNull(request, "Request must not be null");

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Workspace workspace = workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + request.workspaceId()));

        Optional<WorkspaceMember> existing = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), targetUser.getId());
        if (existing.isPresent()) {
            WorkspaceMember member = existing.get();
            member.setRole(request.role());
            workspaceMemberRepository.save(member);
        } else {
            WorkspaceMember newMember = new WorkspaceMember();
            newMember.setWorkspace(workspace);
            newMember.setUser(targetUser);
            newMember.setRole(request.role());
            workspaceMemberRepository.save(newMember);
        }

        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_UPDATED,
                null,
                workspace.getId(),
                null,
                adminUser.getUsername(),
                "Assigned role " + request.role() + " in workspace '" + workspace.getName() + "' to " + targetUser.getUsername()
        );

        return buildUserResponse(targetUser);
    }

    @Override
    public AdminUserResponse removeWorkspaceMembership(Long userId, Long workspaceId, User adminUser) {
        checkAdminAccess(adminUser);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));

        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .ifPresent(workspaceMemberRepository::delete);

        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_UPDATED,
                null,
                workspace.getId(),
                null,
                adminUser.getUsername(),
                "Removed membership from workspace '" + workspace.getName() + "' for " + targetUser.getUsername()
        );

        return buildUserResponse(targetUser);
    }

    private AdminUserResponse buildUserResponse(User user) {
        List<UserWorkspaceMembershipDto> memberships = workspaceMemberRepository.findByUserId(user.getId())
                .stream()
                .map(wm -> new UserWorkspaceMembershipDto(
                        wm.getWorkspace().getId(),
                        wm.getWorkspace().getName(),
                        wm.getRole(),
                        wm.getJoinedAt()
                ))
                .toList();

        return AdminUserResponse.fromUser(user, memberships);
    }
}
