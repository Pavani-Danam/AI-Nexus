package com.ainexus.service;

import com.ainexus.dto.AdminDashboardSummaryResponse;
import com.ainexus.entity.Role;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowExecution;
import com.ainexus.entity.WorkflowExecutionStatus;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.*;
import com.ainexus.service.impl.AdminDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowExecutionRepository executionRepository;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("sysadmin");
        adminUser.setRole(Role.ROLE_ADMIN);
        adminUser.setEnabled(true);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("member");
        regularUser.setRole(Role.ROLE_USER);
        regularUser.setEnabled(true);
    }

    @Test
    @DisplayName("TEST 1: Admin successfully retrieves enterprise dashboard summary")
    void testGetDashboardSummaryAsAdmin() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.findAll()).thenReturn(List.of(adminUser, regularUser));
        when(workspaceRepository.count()).thenReturn(4L);
        when(documentRepository.count()).thenReturn(25L);
        when(workflowRepository.count()).thenReturn(8L);

        WorkflowExecution execSuccess = new WorkflowExecution();
        execSuccess.setStatus(WorkflowExecutionStatus.COMPLETED);

        WorkflowExecution execFailed = new WorkflowExecution();
        execFailed.setStatus(WorkflowExecutionStatus.FAILED);

        when(executionRepository.findAll()).thenReturn(List.of(execSuccess, execFailed));

        AdminDashboardSummaryResponse response = adminDashboardService.getDashboardSummary(adminUser);

        assertNotNull(response);
        assertEquals(10L, response.totalUsers());
        assertEquals(2L, response.activeUsers());
        assertEquals(4L, response.totalWorkspaces());
        assertEquals(25L, response.totalDocuments());
        assertEquals(8L, response.totalWorkflows());
        assertEquals(2L, response.totalExecutions());
        assertEquals(1L, response.successfulExecutions());
        assertEquals(1L, response.failedExecutions());
        assertEquals("HEALTHY", response.systemHealthStatus());
        assertNotNull(response.systemMetrics());
    }

    @Test
    @DisplayName("TEST 2: Regular user access to admin dashboard is denied with 403 Forbidden")
    void testDenyRegularUserAccess() {
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class,
                () -> adminDashboardService.getDashboardSummary(regularUser));

        assertTrue(exception.getMessage().contains("Administrator privileges required"));
        verifyNoInteractions(workspaceRepository, documentRepository, workflowRepository, executionRepository);
    }

    @Test
    @DisplayName("TEST 3: Verify isAdmin check logic")
    void testIsAdminValidation() {
        assertTrue(adminDashboardService.isAdmin(adminUser));
        assertFalse(adminDashboardService.isAdmin(regularUser));
        assertFalse(adminDashboardService.isAdmin(null));
    }
}
