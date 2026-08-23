package com.ainexus.service;

import com.ainexus.dto.*;
import com.ainexus.entity.*;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkflowApprovalRepository;
import com.ainexus.repository.WorkflowExecutionRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.impl.WorkflowApprovalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowApprovalServiceTest {

    @Mock
    private WorkflowApprovalRepository approvalRepository;

    @Mock
    private WorkflowExecutionRepository executionRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private PlanExecutionService planExecutionService;

    @InjectMocks
    private WorkflowApprovalServiceImpl workflowApprovalService;

    private User owner;
    private User intruder;
    private Workspace testWorkspace;
    private Workflow testWorkflow;
    private WorkflowExecution testExecution;
    private WorkflowStep testStep;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(10L);
        owner.setUsername("alice");

        intruder = new User();
        intruder.setId(20L);
        intruder.setUsername("bob");

        testWorkspace = new Workspace();
        testWorkspace.setId(100L);
        testWorkspace.setName("Engineering");
        testWorkspace.setOwner(owner);

        testWorkflow = new Workflow("Deployment Workflow", "Deploy app", testWorkspace, owner);
        testWorkflow.setId(1L);

        testExecution = new WorkflowExecution(testWorkflow, testWorkspace, owner);
        testExecution.setId(500L);
        testExecution.setStatus(WorkflowExecutionStatus.RUNNING);

        testStep = new WorkflowStep("deploy-prod", "Deploy to Production", WorkflowStepType.SYNTHESIZE, "{}", 2, List.of(), true);
    }

    @Test
    @DisplayName("TEST 1: Request approval successfully pauses workflow")
    void testRequestApprovalPausesWorkflow() {
        when(approvalRepository.save(any(WorkflowApproval.class))).thenAnswer(inv -> {
            WorkflowApproval a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        WorkflowApprovalResponse response = workflowApprovalService.requestApproval(
                testExecution, testStep, owner, "Production deployment approval required", LocalDateTime.now().plusHours(2));

        assertNotNull(response);
        assertEquals(WorkflowApprovalStatus.PENDING, response.status());
        assertEquals(WorkflowExecutionStatus.WAITING_FOR_APPROVAL, testExecution.getStatus());
        verify(executionRepository, times(1)).save(testExecution);
    }

    @Test
    @DisplayName("TEST 2: Approve step resumes and completes workflow")
    void testApproveStepResumesWorkflow() {
        WorkflowApproval approval = new WorkflowApproval(testExecution, testWorkspace, "deploy-prod", "Deploy to Production", owner, "Reason", null);
        approval.setId(1L);

        when(approvalRepository.findById(1L)).thenReturn(Optional.of(approval));
        when(planExecutionService.executePlan(any(AgentPlan.class), eq(owner))).thenReturn(
                new AgentExecutionResult("exec-resume", "plan-resume", PlanExecutionStatus.COMPLETED, "Deployment Succeeded", List.of(), Map.of())
        );
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowExecutionResponse response = workflowApprovalService.approveStep(
                1L, new WorkflowApprovalDecisionRequest("Approved by Lead"), owner);

        assertNotNull(response);
        assertEquals(WorkflowExecutionStatus.COMPLETED, response.status());
        assertEquals(WorkflowApprovalStatus.APPROVED, approval.getStatus());
        assertEquals("Approved by Lead", approval.getResolutionComment());
    }

    @Test
    @DisplayName("TEST 3: Reject step stops workflow safely")
    void testRejectStepStopsWorkflow() {
        WorkflowApproval approval = new WorkflowApproval(testExecution, testWorkspace, "deploy-prod", "Deploy to Production", owner, "Reason", null);
        approval.setId(1L);

        when(approvalRepository.findById(1L)).thenReturn(Optional.of(approval));
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowExecutionResponse response = workflowApprovalService.rejectStep(
                1L, new WorkflowApprovalDecisionRequest("Risky release"), owner);

        assertNotNull(response);
        assertEquals(WorkflowExecutionStatus.CANCELLED, response.status());
        assertEquals(WorkflowApprovalStatus.REJECTED, approval.getStatus());
        assertTrue(response.errorMessage().contains("Risky release"));
    }

    @Test
    @DisplayName("TEST 4: Reject approval action by unauthorized intruder")
    void testUnauthorizedApprovalFails() {
        WorkflowApproval approval = new WorkflowApproval(testExecution, testWorkspace, "deploy-prod", "Deploy to Production", owner, "Reason", null);
        approval.setId(1L);

        when(approvalRepository.findById(1L)).thenReturn(Optional.of(approval));

        assertThrows(UnauthorizedAccessException.class,
                () -> workflowApprovalService.approveStep(1L, null, intruder));
    }

    @Test
    @DisplayName("TEST 5: Prevent duplicate resolution on resolved approval")
    void testDuplicateApprovalFails() {
        WorkflowApproval approval = new WorkflowApproval(testExecution, testWorkspace, "deploy-prod", "Deploy to Production", owner, "Reason", null);
        approval.setId(1L);
        approval.setStatus(WorkflowApprovalStatus.APPROVED);

        when(approvalRepository.findById(1L)).thenReturn(Optional.of(approval));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> workflowApprovalService.approveStep(1L, null, owner));
        assertTrue(ex.getMessage().contains("already resolved"));
    }

    @Test
    @DisplayName("TEST 6: Expired approvals are automatically resolved and cancelled")
    void testProcessExpiredApprovals() {
        WorkflowApproval expiredApproval = new WorkflowApproval(
                testExecution, testWorkspace, "deploy-prod", "Deploy to Production", owner, "Reason", LocalDateTime.now().minusMinutes(5));
        expiredApproval.setId(1L);
        testExecution.setStatus(WorkflowExecutionStatus.WAITING_FOR_APPROVAL);

        when(approvalRepository.findExpiredApprovals(any())).thenReturn(List.of(expiredApproval));

        workflowApprovalService.processExpiredApprovals();

        assertEquals(WorkflowApprovalStatus.EXPIRED, expiredApproval.getStatus());
        assertEquals(WorkflowExecutionStatus.CANCELLED, testExecution.getStatus());
        verify(approvalRepository, times(1)).save(expiredApproval);
    }
}
