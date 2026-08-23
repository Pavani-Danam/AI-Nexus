package com.ainexus.service;

import com.ainexus.dto.AdminCreateWorkspaceRequest;
import com.ainexus.dto.AdminUpdateWorkspaceRequest;
import com.ainexus.dto.AdminWorkspaceDetailResponse;
import com.ainexus.dto.ManageWorkspaceMembershipRequest;
import com.ainexus.entity.Role;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.entity.WorkspaceMember;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.*;
import com.ainexus.service.impl.AdminWorkspaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminWorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkflowMonitoringService monitoringService;

    @InjectMocks
    private AdminWorkspaceServiceImpl adminWorkspaceService;

    private User admin;
    private User regularUser;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setEnabled(true);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("alice");
        regularUser.setRole(Role.ROLE_USER);
        regularUser.setEnabled(true);

        workspace = Workspace.builder()
                .id(100L)
                .name("Engineering Hub")
                .description("Core engineering repository")
                .owner(admin)
                .build();
    }

    @Test
    @DisplayName("TEST 1: Admin lists and filters workspaces")
    void testListWorkspaces() {
        when(workspaceRepository.findAll()).thenReturn(List.of(workspace));
        when(workspaceMemberRepository.findByWorkspace(workspace)).thenReturn(List.of());
        when(documentRepository.findByWorkspace(workspace)).thenReturn(List.of());
        when(workflowRepository.findAll()).thenReturn(List.of());

        Page<AdminWorkspaceDetailResponse> result = adminWorkspaceService.listWorkspaces("eng", PageRequest.of(0, 10), admin);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Engineering Hub", result.getContent().get(0).name());
    }

    @Test
    @DisplayName("TEST 2: Regular user is denied access to admin workspace features")
    void testDenyRegularUser() {
        assertThrows(UnauthorizedAccessException.class,
                () -> adminWorkspaceService.listWorkspaces(null, PageRequest.of(0, 10), regularUser));
    }

    @Test
    @DisplayName("TEST 3: Admin creates a new workspace and initializes owner membership")
    void testCreateWorkspace() {
        AdminCreateWorkspaceRequest request = new AdminCreateWorkspaceRequest("Data Ops", "Operations tenant", 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        Workspace created = Workspace.builder()
                .id(101L)
                .name("Data Ops")
                .description("Operations tenant")
                .owner(regularUser)
                .build();
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(created);
        when(workspaceMemberRepository.findByWorkspace(created)).thenReturn(List.of());
        when(documentRepository.findByWorkspace(created)).thenReturn(List.of());
        when(workflowRepository.findAll()).thenReturn(List.of());

        AdminWorkspaceDetailResponse res = adminWorkspaceService.createWorkspace(request, admin);

        assertNotNull(res);
        assertEquals("Data Ops", res.name());
        verify(workspaceMemberRepository, times(1)).save(any(WorkspaceMember.class));
        verify(monitoringService, times(1)).recordAuditEvent(any(), any(), any(), any(), eq("admin"), anyString());
    }

    @Test
    @DisplayName("TEST 4: Admin updates workspace metadata")
    void testUpdateWorkspace() {
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(workspace);
        when(workspaceMemberRepository.findByWorkspace(workspace)).thenReturn(List.of());
        when(documentRepository.findByWorkspace(workspace)).thenReturn(List.of());
        when(workflowRepository.findAll()).thenReturn(List.of());

        AdminUpdateWorkspaceRequest req = new AdminUpdateWorkspaceRequest("Engineering Core", "Updated description");
        AdminWorkspaceDetailResponse res = adminWorkspaceService.updateWorkspace(100L, req, admin);

        assertNotNull(res);
        assertEquals("Engineering Core", res.name());
    }

    @Test
    @DisplayName("TEST 5: Admin manages member in workspace")
    void testAddOrUpdateMember() {
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(regularUser);
        member.setRole("EDITOR");

        when(workspaceMemberRepository.findByWorkspace(workspace)).thenReturn(List.of(member));
        when(documentRepository.findByWorkspace(workspace)).thenReturn(List.of());
        when(workflowRepository.findAll()).thenReturn(List.of());

        ManageWorkspaceMembershipRequest req = new ManageWorkspaceMembershipRequest(100L, "EDITOR");
        AdminWorkspaceDetailResponse res = adminWorkspaceService.addOrUpdateMember(100L, req, 2L, admin);

        assertNotNull(res);
        assertEquals(1, res.memberCount());
    }
}
