package com.ainexus.service;

import com.ainexus.dto.WorkflowExecutionResponse;
import com.ainexus.dto.WorkflowMonitoringSummaryResponse;
import com.ainexus.entity.*;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.*;
import com.ainexus.service.impl.WorkflowMonitoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowMonitoringServiceTest {

    @Mock
    private WorkflowAuditEventRepository auditRepository;

    @Mock
    private WorkflowExecutionRepository executionRepository;

    @Mock
    private WorkflowApprovalRepository approvalRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private WorkflowMonitoringServiceImpl monitoringService;

    private User owner;
    private User intruder;
    private Workspace testWorkspace;
    private Workflow testWorkflow;
    private WorkflowExecution execution1;
    private WorkflowExecution execution2;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("alice");

        intruder = new User();
        intruder.setId(2L);
        intruder.setUsername("bob");

        testWorkspace = new Workspace();
        testWorkspace.setId(100L);
        testWorkspace.setName("Ops Space");
        testWorkspace.setOwner(owner);

        testWorkflow = new Workflow("Nightly ETL", "Batch pipeline", testWorkspace, owner);
        testWorkflow.setId(10L);

        execution1 = new WorkflowExecution(testWorkflow, testWorkspace, owner);
        execution1.setId(501L);
        execution1.setStatus(WorkflowExecutionStatus.COMPLETED);
        execution1.setDurationMs(1200L);
        execution1.setStartTime(LocalDateTime.now().minusSeconds(10));
        execution1.setEndTime(LocalDateTime.now());
        execution1.setStepExecutions(new ArrayList<>());

        execution2 = new WorkflowExecution(testWorkflow, testWorkspace, owner);
        execution2.setId(502L);
        execution2.setStatus(WorkflowExecutionStatus.FAILED);
        execution2.setErrorMessage("Service unavailable");
        execution2.setDurationMs(800L);
        execution2.setStartTime(LocalDateTime.now().minusSeconds(5));
        execution2.setEndTime(LocalDateTime.now());
        execution2.setStepExecutions(new ArrayList<>());
    }

    @Test
    @DisplayName("TEST 1: Record audit event successfully")
    void testRecordAuditEvent() {
        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_EXECUTED,
                10L, 100L, 501L, "alice", "Execution started"
        );
        verify(auditRepository, times(1)).save(any(WorkflowAuditEvent.class));
    }

    @Test
    @DisplayName("TEST 2: Get paginated and filtered execution history")
    void testGetExecutionHistoryWithFilters() {
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(executionRepository.findByWorkspaceIdOrderByStartTimeDesc(100L))
                .thenReturn(List.of(execution1, execution2));

        Pageable pageable = PageRequest.of(0, 10);
        Page<WorkflowExecutionResponse> result = monitoringService.getExecutionHistory(
                100L, 10L, WorkflowExecutionStatus.COMPLETED, pageable, owner
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(501L, result.getContent().get(0).id());
        assertEquals(WorkflowExecutionStatus.COMPLETED, result.getContent().get(0).status());
    }

    @Test
    @DisplayName("TEST 3: Get workspace monitoring summary metrics")
    void testGetWorkspaceMonitoringSummary() {
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(executionRepository.findByWorkspaceIdOrderByStartTimeDesc(100L))
                .thenReturn(List.of(execution1, execution2));
        when(approvalRepository.findByWorkspaceIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of());

        WorkflowMonitoringSummaryResponse summary = monitoringService.getWorkspaceMonitoringSummary(100L, owner);

        assertNotNull(summary);
        assertEquals(2, summary.totalExecutions());
        assertEquals(1, summary.successfulExecutions());
        assertEquals(1, summary.failedExecutions());
        assertEquals(0, summary.pendingApprovals());
        assertEquals(1000.0, summary.avgDurationMs());
    }

    @Test
    @DisplayName("TEST 4: Deny unauthorized user access to execution history")
    void testDenyUnauthorizedAccess() {
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));

        assertThrows(UnauthorizedAccessException.class, () ->
                monitoringService.getExecutionHistory(100L, null, null, PageRequest.of(0, 10), intruder)
        );
    }
}
