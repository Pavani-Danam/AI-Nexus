package com.ainexus.service;

import com.ainexus.dto.*;
import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkflowExecutionRepository;
import com.ainexus.repository.WorkflowRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.impl.WorkflowExecutionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutionServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowExecutionRepository workflowExecutionRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private PlanExecutionService planExecutionService;

    @InjectMocks
    private WorkflowExecutionServiceImpl workflowExecutionService;

    private User owner;
    private User intruder;
    private Workspace testWorkspace;
    private Workflow testWorkflow;

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

        testWorkflow = new Workflow("Q3 Review Workflow", "Review metrics", testWorkspace, owner);
        testWorkflow.setId(1L);

        WorkflowStep step1 = new WorkflowStep("s1", "Search Docs", WorkflowStepType.SEARCH, "{}", 1, List.of(), true);
        WorkflowStep step2 = new WorkflowStep("s2", "Analyze Data", WorkflowStepType.ANALYZE, "{}", 2, List.of("s1"), true);
        WorkflowStep step3 = new WorkflowStep("s3", "Synthesize Report", WorkflowStepType.SYNTHESIZE, "{}", 3, List.of("s2"), true);

        testWorkflow.addStep(step1);
        testWorkflow.addStep(step2);
        testWorkflow.addStep(step3);
    }

    @Test
    @DisplayName("TEST 1: Execute multi-step workflow successfully")
    void testExecuteMultiStepWorkflowSuccessfully() {
        WorkflowExecutionRequest execReq = new WorkflowExecutionRequest("Generate Q3 Summary", Map.of());

        when(workflowRepository.findByIdWithSteps(1L)).thenReturn(Optional.of(testWorkflow));
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(workflowExecutionRepository.save(any(WorkflowExecution.class))).thenAnswer(inv -> {
            WorkflowExecution we = inv.getArgument(0);
            if (we.getId() == null) we.setId(500L);
            return we;
        });

        Map<String, TaskExecutionResult> taskResults = Map.of(
                "s1", new TaskExecutionResult("s1", TaskExecutionStatus.SUCCESS, "Found 5 docs", null, 1, 100L, null),
                "s2", new TaskExecutionResult("s2", TaskExecutionStatus.SUCCESS, "Analysis complete", null, 1, 150L, null),
                "s3", new TaskExecutionResult("s3", TaskExecutionStatus.SUCCESS, "Final Report generated", null, 1, 120L, null)
        );

        PlanExecutionResult planResult = new PlanExecutionResult(
                "exec-500",
                "plan-500",
                ExecutionStatus.COMPLETED,
                taskResults,
                "Comprehensive Q3 synthesis completed successfully.",
                null,
                370L
        );

        when(planExecutionService.executePlan(any(AgentPlan.class), eq(owner))).thenReturn(planResult);

        WorkflowExecutionResponse response = workflowExecutionService.executeWorkflow(1L, execReq, owner);

        assertNotNull(response);
        assertEquals(WorkflowExecutionStatus.COMPLETED, response.status());
        assertEquals("Comprehensive Q3 synthesis completed successfully.", response.finalOutput());
        assertEquals(3, response.stepExecutions().size());
        verify(planExecutionService, times(1)).executePlan(any(AgentPlan.class), eq(owner));
    }

    @Test
    @DisplayName("TEST 2: Execute parallel independent steps successfully")
    void testExecuteParallelIndependentSteps() {
        Workflow parallelWf = new Workflow("Parallel Ingestion", "Parallel ingest", testWorkspace, owner);
        parallelWf.setId(2L);
        WorkflowStep stepA = new WorkflowStep("sa", "Search A", WorkflowStepType.SEARCH, "{}", 1, List.of(), true);
        WorkflowStep stepB = new WorkflowStep("sb", "Search B", WorkflowStepType.SEARCH, "{}", 1, List.of(), true);
        parallelWf.addStep(stepA);
        parallelWf.addStep(stepB);

        when(workflowRepository.findByIdWithSteps(2L)).thenReturn(Optional.of(parallelWf));
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(workflowExecutionRepository.save(any(WorkflowExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, TaskExecutionResult> taskResults = Map.of(
                "sa", new TaskExecutionResult("sa", TaskExecutionStatus.SUCCESS, "Result A", null, 1, 50L, null),
                "sb", new TaskExecutionResult("sb", TaskExecutionStatus.SUCCESS, "Result B", null, 1, 60L, null)
        );

        PlanExecutionResult planResult = new PlanExecutionResult(
                "exec-parallel",
                "plan-parallel",
                ExecutionStatus.COMPLETED,
                taskResults,
                "Both branches completed.",
                null,
                110L
        );

        when(planExecutionService.executePlan(any(AgentPlan.class), eq(owner))).thenReturn(planResult);

        WorkflowExecutionResponse response = workflowExecutionService.executeWorkflow(2L, null, owner);

        assertNotNull(response);
        assertEquals(WorkflowExecutionStatus.COMPLETED, response.status());
        assertEquals(2, response.stepExecutions().size());
    }

    @Test
    @DisplayName("TEST 3: Handle failed workflow step and record error output")
    void testWorkflowExecutionWithFailedStep() {
        when(workflowRepository.findByIdWithSteps(1L)).thenReturn(Optional.of(testWorkflow));
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(workflowExecutionRepository.save(any(WorkflowExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, TaskExecutionResult> taskResults = Map.of(
                "s1", new TaskExecutionResult("s1", TaskExecutionStatus.SUCCESS, "Found docs", null, 1, 80L, null),
                "s2", new TaskExecutionResult("s2", TaskExecutionStatus.FAILED, null, "Model timed out", 3, 300L, TaskFailureCategory.TRANSIENT_FAILURE),
                "s3", new TaskExecutionResult("s3", TaskExecutionStatus.SKIPPED, null, "Skipped due to upstream failure", 0, 0L, null)
        );

        PlanExecutionResult planResult = new PlanExecutionResult(
                "exec-fail",
                "plan-fail",
                ExecutionStatus.FAILED,
                taskResults,
                null,
                "Step s2 failed",
                380L
        );

        when(planExecutionService.executePlan(any(AgentPlan.class), eq(owner))).thenReturn(planResult);

        WorkflowExecutionResponse response = workflowExecutionService.executeWorkflow(1L, null, owner);

        assertNotNull(response);
        assertEquals(WorkflowExecutionStatus.FAILED, response.status());
        assertEquals("Step s2 failed", response.errorMessage());
    }

    @Test
    @DisplayName("TEST 4: Reject workflow execution for unauthorized user")
    void testRejectUnauthorizedWorkflowExecution() {
        when(workflowRepository.findByIdWithSteps(1L)).thenReturn(Optional.of(testWorkflow));
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));

        assertThrows(UnauthorizedAccessException.class, () -> workflowExecutionService.executeWorkflow(1L, null, intruder));
        verify(planExecutionService, never()).executePlan(any(), any());
    }

    @Test
    @DisplayName("TEST 5: Reject execution when workflow has no enabled steps")
    void testRejectWorkflowWithNoEnabledSteps() {
        Workflow emptyWf = new Workflow("Empty WF", "No steps", testWorkspace, owner);
        emptyWf.setId(3L);

        when(workflowRepository.findByIdWithSteps(3L)).thenReturn(Optional.of(emptyWf));
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> workflowExecutionService.executeWorkflow(3L, null, owner));
        assertTrue(ex.getMessage().contains("Cannot execute workflow without enabled steps"));
    }

    @Test
    @DisplayName("TEST 6: Get execution by ID and check authorization")
    void testGetExecutionById() {
        WorkflowExecution we = new WorkflowExecution(testWorkflow, testWorkspace, owner);
        we.setId(88L);
        we.setStatus(WorkflowExecutionStatus.COMPLETED);

        when(workflowExecutionRepository.findByIdWithSteps(88L)).thenReturn(Optional.of(we));
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));

        WorkflowExecutionResponse response = workflowExecutionService.getExecutionById(88L, owner);

        assertNotNull(response);
        assertEquals(88L, response.id());
        assertEquals(WorkflowExecutionStatus.COMPLETED, response.status());
    }
}
