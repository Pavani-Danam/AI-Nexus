package com.ainexus.service.impl;

import com.ainexus.dto.AdminCreateWorkspaceRequest;
import com.ainexus.dto.AdminUpdateWorkspaceRequest;
import com.ainexus.dto.AdminWorkspaceDetailResponse;
import com.ainexus.dto.AdminWorkspaceMemberDto;
import com.ainexus.dto.ManageWorkspaceMembershipRequest;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowAuditEventType;
import com.ainexus.entity.Workspace;
import com.ainexus.entity.WorkspaceMember;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.DocumentRepository;
import com.ainexus.repository.UserRepository;
import com.ainexus.repository.WorkflowRepository;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.AdminWorkspaceService;
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
public class AdminWorkspaceServiceImpl implements AdminWorkspaceService {

    private static final Logger logger = LoggerFactory.getLogger(AdminWorkspaceServiceImpl.class);

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final DocumentRepository documentRepository;
    private final WorkflowRepository workflowRepository;
    private final UserRepository userRepository;
    private final WorkflowMonitoringService monitoringService;

    public AdminWorkspaceServiceImpl(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            DocumentRepository documentRepository,
            WorkflowRepository workflowRepository,
            UserRepository userRepository,
            WorkflowMonitoringService monitoringService) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.documentRepository = documentRepository;
        this.workflowRepository = workflowRepository;
        this.userRepository = userRepository;
        this.monitoringService = monitoringService;
    }

    private void checkAdminAccess(User user) {
        Objects.requireNonNull(user, "User must not be null");
        String roleStr = user.getRole() != null ? user.getRole().name() : "";
        if (!roleStr.contains("ADMIN")) {
            logger.warn("[SECURITY] Access denied. User '{}' lacks administrative authority.", user.getUsername());
            throw new UnauthorizedAccessException("Forbidden: Administrator privileges required.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminWorkspaceDetailResponse> listWorkspaces(String query, Pageable pageable, User adminUser) {
        checkAdminAccess(adminUser);

        List<Workspace> allWorkspaces = workspaceRepository.findAll();
        List<Workspace> filtered = allWorkspaces.stream()
                .filter(w -> {
                    if (query == null || query.isBlank()) return true;
                    String q = query.toLowerCase().trim();
                    boolean matchName = w.getName() != null && w.getName().toLowerCase().contains(q);
                    boolean matchDesc = w.getDescription() != null && w.getDescription().toLowerCase().contains(q);
                    boolean matchOwner = w.getOwner() != null && w.getOwner().getUsername().toLowerCase().contains(q);
                    return matchName || matchDesc || matchOwner;
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<AdminWorkspaceDetailResponse> content = (start <= end && start < filtered.size())
                ? filtered.subList(start, end).stream().map(this::buildDetailResponse).toList()
                : List.of();

        return new PageImpl<>(content, pageable, filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminWorkspaceDetailResponse getWorkspaceDetails(Long workspaceId, User adminUser) {
        checkAdminAccess(adminUser);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));
        return buildDetailResponse(workspace);
    }

    @Override
    public AdminWorkspaceDetailResponse createWorkspace(AdminCreateWorkspaceRequest request, User adminUser) {
        checkAdminAccess(adminUser);
        Objects.requireNonNull(request, "Request must not be null");

        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found with ID: " + request.ownerId()));

        Workspace workspace = Workspace.builder()
                .name(request.name().trim())
                .description(request.description())
                .owner(owner)
                .build();

        Workspace saved = workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(saved);
        member.setUser(owner);
        member.setRole("ADMIN");
        workspaceMemberRepository.save(member);

        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_CREATED,
                null,
                saved.getId(),
                null,
                adminUser.getUsername(),
                "Admin created workspace '" + saved.getName() + "' with owner " + owner.getUsername()
        );

        return buildDetailResponse(saved);
    }

    @Override
    public AdminWorkspaceDetailResponse updateWorkspace(Long workspaceId, AdminUpdateWorkspaceRequest request, User adminUser) {
        checkAdminAccess(adminUser);
        Objects.requireNonNull(request, "Request must not be null");

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));

        workspace.setName(request.name().trim());
        workspace.setDescription(request.description());
        Workspace updated = workspaceRepository.save(workspace);

        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_UPDATED,
                null,
                updated.getId(),
                null,
                adminUser.getUsername(),
                "Admin updated metadata for workspace '" + updated.getName() + "'"
        );

        return buildDetailResponse(updated);
    }

    @Override
    public AdminWorkspaceDetailResponse addOrUpdateMember(
            Long workspaceId,
            ManageWorkspaceMembershipRequest request,
            Long targetUserId,
            User adminUser) {
        checkAdminAccess(adminUser);
        Objects.requireNonNull(request, "Request must not be null");

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with ID: " + targetUserId));

        Optional<WorkspaceMember> existing = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId);
        if (existing.isPresent()) {
            WorkspaceMember member = existing.get();
            member.setRole(request.role());
            workspaceMemberRepository.save(member);
        } else {
            WorkspaceMember member = new WorkspaceMember();
            member.setWorkspace(workspace);
            member.setUser(targetUser);
            member.setRole(request.role());
            workspaceMemberRepository.save(member);
        }

        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_UPDATED,
                null,
                workspace.getId(),
                null,
                adminUser.getUsername(),
                "Admin assigned role '" + request.role() + "' to user " + targetUser.getUsername() + " in workspace " + workspace.getName()
        );

        return buildDetailResponse(workspace);
    }

    @Override
    public AdminWorkspaceDetailResponse removeMember(Long workspaceId, Long targetUserId, User adminUser) {
        checkAdminAccess(adminUser);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with ID: " + targetUserId));

        if (workspace.getOwner() != null && workspace.getOwner().getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot remove the workspace owner from members list.");
        }

        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .ifPresent(workspaceMemberRepository::delete);

        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_UPDATED,
                null,
                workspace.getId(),
                null,
                adminUser.getUsername(),
                "Admin removed member " + targetUser.getUsername() + " from workspace " + workspace.getName()
        );

        return buildDetailResponse(workspace);
    }

    private AdminWorkspaceDetailResponse buildDetailResponse(Workspace workspace) {
        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspace(workspace);
        long memberCount = members.size();

        long docCount = 0;
        try {
            docCount = documentRepository.findByWorkspace(workspace).size();
        } catch (Exception ignored) {
            docCount = 0;
        }

        long wfCount = 0;
        try {
            wfCount = workflowRepository.findAll().stream()
                    .filter(w -> w.getWorkspace() != null && w.getWorkspace().getId().equals(workspace.getId()))
                    .count();
        } catch (Exception ignored) {
            wfCount = 0;
        }

        List<AdminWorkspaceMemberDto> memberDtos = members.stream()
                .map(m -> new AdminWorkspaceMemberDto(
                        m.getUser().getId(),
                        m.getUser().getUsername(),
                        m.getUser().getName(),
                        m.getUser().getEmail(),
                        m.getRole(),
                        m.getJoinedAt()
                ))
                .toList();

        return new AdminWorkspaceDetailResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getOwner() != null ? workspace.getOwner().getId() : null,
                workspace.getOwner() != null ? workspace.getOwner().getUsername() : "N/A",
                memberCount,
                docCount,
                wfCount,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt(),
                memberDtos
        );
    }
}
