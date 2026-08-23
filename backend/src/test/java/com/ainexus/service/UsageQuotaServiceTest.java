package com.ainexus.service;

import com.ainexus.dto.RecordUsageRequest;
import com.ainexus.dto.UpdateWorkspaceQuotaRequest;
import com.ainexus.dto.WorkspaceQuotaResponse;
import com.ainexus.entity.Role;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.entity.WorkspaceQuota;
import com.ainexus.exception.QuotaExceededException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.repository.WorkspaceQuotaRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.impl.UsageQuotaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageQuotaServiceTest {

    @Mock
    private WorkspaceQuotaRepository quotaRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private WorkflowMonitoringService monitoringService;

    @InjectMocks
    private UsageQuotaServiceImpl usageQuotaService;

    private User admin;
    private User regularUser;
    private Workspace workspace;
    private WorkspaceQuota quota;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setEnabled(true);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("member");
        regularUser.setRole(Role.ROLE_USER);
        regularUser.setEnabled(true);

        workspace = Workspace.builder()
                .id(10L)
                .name("Test Tenant")
                .owner(admin)
                .build();

        quota = WorkspaceQuota.builder()
                .id(100L)
                .workspace(workspace)
                .maxAiRequests(50)
                .maxTokens(10000)
                .maxDocumentProcessing(5)
                .maxEmbeddings(100)
                .maxVectorOperations(200)
                .maxWorkflowExecutions(10)
                .maxAgentExecutions(10)
                .usedAiRequests(10)
                .usedTokens(2000)
                .usedDocumentProcessing(1)
                .usedEmbeddings(20)
                .usedVectorOperations(50)
                .usedWorkflowExecutions(2)
                .usedAgentExecutions(1)
                .build();
    }

    @Test
    @DisplayName("TEST 1: Admin lists all workspace quotas")
    void testListAllQuotas() {
        when(quotaRepository.findAll()).thenReturn(List.of(quota));

        List<WorkspaceQuotaResponse> list = usageQuotaService.listAllWorkspaceQuotas(admin);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).workspaceId());
        assertEquals(40L, list.get(0).remainingAiRequests());
    }

    @Test
    @DisplayName("TEST 2: Regular non-member user access is denied")
    void testDenyNonMemberUser() {
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class,
                () -> usageQuotaService.getWorkspaceQuota(10L, regularUser));
    }

    @Test
    @DisplayName("TEST 3: Check and record usage successfully increments counters")
    void testCheckAndRecordUsageSuccess() {
        when(quotaRepository.findByWorkspace_Id(10L)).thenReturn(Optional.of(quota));
        when(quotaRepository.save(any(WorkspaceQuota.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordUsageRequest request = new RecordUsageRequest(5, 500, 1, 10, 20, 1, 1);
        usageQuotaService.checkAndRecordUsage(10L, request);

        assertEquals(15L, quota.getUsedAiRequests());
        assertEquals(2500L, quota.getUsedTokens());
        assertEquals(2L, quota.getUsedDocumentProcessing());
    }

    @Test
    @DisplayName("TEST 4: Exceeding quota throws QuotaExceededException")
    void testQuotaExceededThrowsException() {
        when(quotaRepository.findByWorkspace_Id(10L)).thenReturn(Optional.of(quota));

        RecordUsageRequest request = new RecordUsageRequest(100, 0, 0, 0, 0, 0, 0); // limit is 50, currently 10

        assertThrows(QuotaExceededException.class,
                () -> usageQuotaService.checkAndRecordUsage(10L, request));
    }

    @Test
    @DisplayName("TEST 5: Admin updates quota limits")
    void testUpdateQuotaLimits() {
        when(quotaRepository.findByWorkspace_Id(10L)).thenReturn(Optional.of(quota));
        when(quotaRepository.save(any(WorkspaceQuota.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateWorkspaceQuotaRequest req = new UpdateWorkspaceQuotaRequest(500L, null, null, null, null, null, null);
        WorkspaceQuotaResponse res = usageQuotaService.updateWorkspaceQuota(10L, req, admin);

        assertNotNull(res);
        assertEquals(500L, res.maxAiRequests());
        verify(monitoringService, times(1)).recordAuditEvent(any(), any(), any(), any(), eq("admin"), anyString());
    }

    @Test
    @DisplayName("TEST 6: Admin resets workspace usage")
    void testResetUsage() {
        when(quotaRepository.findByWorkspace_Id(10L)).thenReturn(Optional.of(quota));
        when(quotaRepository.save(any(WorkspaceQuota.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceQuotaResponse res = usageQuotaService.resetWorkspaceUsage(10L, admin);

        assertNotNull(res);
        assertEquals(0L, res.usedAiRequests());
        assertEquals(0L, res.usedTokens());
        assertEquals(0L, res.usedDocumentProcessing());
    }
}
