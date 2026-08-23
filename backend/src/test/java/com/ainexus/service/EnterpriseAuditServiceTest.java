package com.ainexus.service;

import com.ainexus.dto.AuditLogResponse;
import com.ainexus.dto.AuditLogSearchRequest;
import com.ainexus.entity.AuditActionType;
import com.ainexus.entity.EnterpriseAuditLog;
import com.ainexus.entity.Role;
import com.ainexus.entity.User;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.EnterpriseAuditLogRepository;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.service.impl.EnterpriseAuditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnterpriseAuditServiceTest {

    @Mock
    private EnterpriseAuditLogRepository auditLogRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private EnterpriseAuditServiceImpl enterpriseAuditService;

    private User admin;
    private User regularUser;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setEnabled(true);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("bob");
        regularUser.setRole(Role.ROLE_USER);
        regularUser.setEnabled(true);
    }

    @Test
    @DisplayName("TEST 1: Log event masks sensitive passwords and bearer tokens")
    void testLogEventMasksSensitiveData() {
        String sensitiveDetails = "User updated password=Secret1234! with Bearer eyJhbGciOiJIUzI1NiJ9.test and apiKey=key-999";
        when(auditLogRepository.save(any(EnterpriseAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        EnterpriseAuditLog log = enterpriseAuditService.logEvent(
                AuditActionType.USER_UPDATED,
                "admin",
                10L,
                "USER",
                "2",
                "SUCCESS",
                sensitiveDetails
        );

        assertNotNull(log);
        assertFalse(log.getSafeDetails().contains("Secret1234!"));
        assertFalse(log.getSafeDetails().contains("key-999"));
        assertTrue(log.getSafeDetails().contains("***REDACTED***"));
    }

    @Test
    @DisplayName("TEST 2: Admin can search global enterprise audit logs")
    void testAdminSearchGlobalAuditLogs() {
        EnterpriseAuditLog log = EnterpriseAuditLog.builder()
                .id(100L)
                .actionType(AuditActionType.LOGIN_SUCCESS)
                .actorUsername("admin")
                .result("SUCCESS")
                .safeDetails("User logged in")
                .build();

        when(auditLogRepository.searchAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log)));

        AuditLogSearchRequest filter = new AuditLogSearchRequest(null, null, null, null, null);
        Page<AuditLogResponse> page = enterpriseAuditService.searchAuditLogs(filter, PageRequest.of(0, 10), admin);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(AuditActionType.LOGIN_SUCCESS, page.getContent().get(0).actionType());
    }

    @Test
    @DisplayName("TEST 3: Non-admin denied from searching global audit logs without workspace scoping")
    void testNonAdminDeniedGlobalSearch() {
        AuditLogSearchRequest filter = new AuditLogSearchRequest(null, null, null, null, null);

        assertThrows(UnauthorizedAccessException.class,
                () -> enterpriseAuditService.searchAuditLogs(filter, PageRequest.of(0, 10), regularUser));
    }

    @Test
    @DisplayName("TEST 4: Non-admin denied from accessing audit logs of non-member workspace")
    void testNonAdminDeniedCrossWorkspaceLogs() {
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(99L, 2L)).thenReturn(false);

        AuditLogSearchRequest filter = new AuditLogSearchRequest(99L, null, null, null, null);

        assertThrows(UnauthorizedAccessException.class,
                () -> enterpriseAuditService.searchAuditLogs(filter, PageRequest.of(0, 10), regularUser));
    }

    @Test
    @DisplayName("TEST 5: Non-admin can inspect audit log within their own workspace")
    void testNonAdminInspectOwnWorkspaceLog() {
        EnterpriseAuditLog log = EnterpriseAuditLog.builder()
                .id(50L)
                .actionType(AuditActionType.DOCUMENT_UPLOAD)
                .actorUsername("bob")
                .workspaceId(10L)
                .result("SUCCESS")
                .safeDetails("Uploaded spec.pdf")
                .build();

        when(auditLogRepository.findById(50L)).thenReturn(Optional.of(log));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(true);

        AuditLogResponse res = enterpriseAuditService.getAuditLogDetails(50L, regularUser);

        assertNotNull(res);
        assertEquals(AuditActionType.DOCUMENT_UPLOAD, res.actionType());
        assertEquals("bob", res.actorUsername());
    }
}
