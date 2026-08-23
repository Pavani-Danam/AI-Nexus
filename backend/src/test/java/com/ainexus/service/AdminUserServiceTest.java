package com.ainexus.service;

import com.ainexus.dto.AdminUserResponse;
import com.ainexus.dto.ManageWorkspaceMembershipRequest;
import com.ainexus.dto.UpdateUserRoleRequest;
import com.ainexus.dto.UpdateUserStatusRequest;
import com.ainexus.entity.Role;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.entity.WorkspaceMember;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.UserRepository;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.impl.AdminUserServiceImpl;
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
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private WorkflowMonitoringService monitoringService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User admin;
    private User regularUser;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setUsername("adminUser");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setEnabled(true);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("john_doe");
        regularUser.setEmail("john@example.com");
        regularUser.setRole(Role.ROLE_USER);
        regularUser.setEnabled(true);

        testWorkspace = new Workspace();
        testWorkspace.setId(10L);
        testWorkspace.setName("Core Workspace");
        testWorkspace.setOwner(admin);
    }

    @Test
    @DisplayName("TEST 1: Admin successfully searches and lists users")
    void testListUsersAsAdmin() {
        when(userRepository.findAll()).thenReturn(List.of(admin, regularUser));
        when(workspaceMemberRepository.findByUserId(anyLong())).thenReturn(List.of());

        Page<AdminUserResponse> result = adminUserService.listUsers("john", PageRequest.of(0, 10), admin);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("john_doe", result.getContent().get(0).username());
    }

    @Test
    @DisplayName("TEST 2: Regular user access to admin user management is forbidden")
    void testDenyRegularUser() {
        assertThrows(UnauthorizedAccessException.class,
                () -> adminUserService.listUsers(null, PageRequest.of(0, 10), regularUser));
    }

    @Test
    @DisplayName("TEST 3: Admin activates/deactivates regular user")
    void testUpdateUserStatus() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceMemberRepository.findByUserId(2L)).thenReturn(List.of());

        AdminUserResponse response = adminUserService.updateUserStatus(2L, new UpdateUserStatusRequest(false), admin);

        assertNotNull(response);
        assertFalse(response.enabled());
        verify(monitoringService, times(1)).recordAuditEvent(any(), any(), any(), any(), eq("adminUser"), anyString());
    }

    @Test
    @DisplayName("TEST 4: Admin cannot deactivate own account")
    void testPreventSelfDeactivation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adminUserService.updateUserStatus(1L, new UpdateUserStatusRequest(false), admin));

        assertTrue(ex.getMessage().contains("cannot deactivate their own account"));
    }

    @Test
    @DisplayName("TEST 5: Admin updates user role successfully")
    void testUpdateUserRole() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceMemberRepository.findByUserId(2L)).thenReturn(List.of());

        AdminUserResponse response = adminUserService.updateUserRole(2L, new UpdateUserRoleRequest(Role.ROLE_ADMIN), admin);

        assertNotNull(response);
        assertEquals(Role.ROLE_ADMIN, response.role());
    }

    @Test
    @DisplayName("TEST 6: Admin assigns workspace membership to user")
    void testAssignWorkspaceMembership() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(testWorkspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(testWorkspace);
        member.setUser(regularUser);
        member.setRole("EDITOR");
        when(workspaceMemberRepository.findByUserId(2L)).thenReturn(List.of(member));

        AdminUserResponse response = adminUserService.assignWorkspaceMembership(
                2L, new ManageWorkspaceMembershipRequest(10L, "EDITOR"), admin
        );

        assertNotNull(response);
        assertEquals(1, response.workspaceMemberships().size());
        assertEquals("EDITOR", response.workspaceMemberships().get(0).memberRole());
    }
}
