package com.ainexus.service;

import com.ainexus.dto.*;
import com.ainexus.entity.*;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkflowApprovalRepository;
import com.ainexus.repository.WorkflowExecutionRepository;
import com.ainexus.service.impl.WorkflowRecoveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowRecoveryServiceTest {

    @Mock
    private WorkflowExecutionRepository executionRepository;

    @Mock
    private WorkflowApprovalRepository approvalRepository;

    @Mock
    private PlanExecutionService planExecutionService;

    @Mock
    private AgentReplanningService replanningService;

    @InjectMocks
    private WorkflowRecoveryServiceImpl recoveryService;

    private User owner;
    private User intruder;
    private Workspace testWorkspace;
    private Workflow testWorkflow;
    private WorkflowExecution testExecution;

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

        testWorkflow = new Workflow("Data Pipeline", "Process Data", testWorkspace, owner);
        testWorkflow.setId(1L);

        WorkflowStep step1 = new WorkflowStep("step-1", "Fetch Data", WorkflowStepType.SEARCH, "{}", 1, List.of(), true);
        WorkflowStep step2 = new WorkflowStep("step-2", "Process Data", WorkflowStepType.ANALYZE, "{}", 2, List.of("step-1"), true);
        testWorkflow.setSteps(List.of(step1, step2));

        testExecution = new WorkflowExecution(testWorkflow, testWorkspace, owner);
        testExecution.setId(500L);
        testExecution.setStatus(WorkflowExecutionStatus.FAILED);
        testExecution.setErrorMessage("Transient network timeout occurred");
        testExecution.setStepExecutions(new ArrayList<>());
    }

    @Test
    @DisplayName("TEST 1: Successfully recover transient failure with idempotency")
    void testRecoverTransientFailureIdempotently() {
        WorkflowStepExecution completedStep1 = new WorkflowStepExecution(
                "step-1", "Fetch Data", WorkflowStepType.SEARCH, 1);
        completedStep1.setStatus(WorkflowExecutionStatus.COMPLETED);
        testExecution.getStepExecutions().add(completedStep1);

        when(executionRepository.findById(500L)).thenReturn(Optional.of(testExecution));
        when(approvalRepository.findByExecutionIdOrderByCreatedAtAsc(500L)).thenReturn(List.of());
        when(planExecutionService.executePlan(any(AgentPlan.class), eq(owner))).thenReturn(
                new AgentExecutionResult("exec-500", "recovery-plan", PlanExecutionStatus.COMPLETED, "Recovery Complete", List.of(), Map.of())
        );
        when(executionRepository.save(any(WorkflowExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowExecutionResponse response = recoveryService.recoverExecution(500L, owner);

        assertNotNull(response);
        assertEquals(WorkflowExecutionStatus.COMPLETED, response.status());
        assertEquals("Recovery Complete", response.finalOutput());
    }

    @Test
    @DisplayName("TEST 2: Refuse recovery on non-recoverable validation/authorization failure")
    void testRefuseNonRecoverableFailure() {
        testExecution.setErrorMessage("Unauthorized access: permission denied");

        when(executionRepository.findById(500L)).thenReturn(Optional.of(testExecution));
        when(approvalRepository.findByExecutionIdOrderByCreatedAtAsc(500L)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> recoveryService.recoverExecution(500L, owner));
        assertTrue(ex.getMessage().contains("Cannot recover non-recoverable failure"));
    }

    @Test
    @DisplayName("TEST 3: Refuse recovery when execution has pending human approvals")
    void testRefuseRecoveryWhenApprovalPending() {
        WorkflowApproval pendingApproval = new WorkflowApproval(
                testExecution, testWorkspace, "step-2", "Process Data", owner, "Pending signoff", null);
        pendingApproval.setStatus(WorkflowApprovalStatus.PENDING);

        when(executionRepository.findById(500L)).thenReturn(Optional.of(testExecution));
        when(approvalRepository.findByExecutionIdOrderByCreatedAtAsc(500L)).thenReturn(List.of(pendingApproval));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> recoveryService.recoverExecution(500L, owner));
        assertTrue(ex.getMessage().contains("pending human approvals"));
    }

    @Test
    @DisplayName("TEST 4: Deny recovery execution for unauthorized user")
    void testDenyUnauthorizedRecovery() {
        when(executionRepository.findById(500L)).thenReturn(Optional.of(testExecution));

        assertThrows(UnauthorizedAccessException.class,
                () -> recoveryService.recoverExecution(500L, intruder));
    }

    @Test
    @DisplayName("TEST 5: Classify failure types accurately")
    void testClassifyFailures() {
        assertEquals(WorkflowFailureType.TIMEOUT_FAILURE, recoveryService.classifyFailure(new RuntimeException("Connection timed out")));
        assertEquals(WorkflowFailureType.RATE_LIMIT_FAILURE, recoveryService.classifyFailure(new RuntimeException("Error 429: Too Many Requests")));
        assertEquals(WorkflowFailureType.AUTHORIZATION_FAILURE, recoveryService.classifyFailure(new RuntimeException("Unauthorized user")));
        assertEquals(WorkflowFailureType.VALIDATION_FAILURE, recoveryService.classifyFailure(new RuntimeException("Validation failed: null value")));
        assertEquals(WorkflowFailureType.PERMANENT_FAILURE, recoveryService.classifyFailure(new RuntimeException("Fatal syntax error")));
    }
}
