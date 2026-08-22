package com.ainexus.service;

import com.ainexus.agent.*;
import com.ainexus.dto.*;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.impl.PlanExecutionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase11Step66PlanExecutionTest {

    @Mock
    private AgentPlanningService planningService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private SearchAgent searchAgent;

    @Mock
    private AnalysisAgent analysisAgent;

    @Mock
    private KnowledgeAgent knowledgeAgent;

    @Mock
    private RAGGenerationService ragGenerationService;

    private PlanExecutionServiceImpl planExecutionService;
    private User testUser;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        planExecutionService = new PlanExecutionServiceImpl(
                planningService,
                workspaceRepository,
                searchAgent,
                analysisAgent,
                knowledgeAgent,
                ragGenerationService
        );
        planExecutionService.setMaxConcurrency(4);
        planExecutionService.setTaskTimeoutSeconds(10);
        planExecutionService.configureRetry(3, 10L, 50L);

        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("alice");

        testWorkspace = new Workspace();
        testWorkspace.setId(1L);
        testWorkspace.setName("Engineering Docs");
        testWorkspace.setOwner(testUser);
    }

    @Test
    @DisplayName("TEST 1: Single-task plan executes successfully")
    void testSingleTaskPlanExecutesSuccessfully() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentPlan plan = new AgentPlan("plan-1", "What is leave policy?", 1L, List.of(task));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(AgentResult.success(AgentType.SEARCH, "t1", "20 days paid leave.", List.of(), Map.of()));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.COMPLETED, result.status());
        assertEquals(1, result.taskResults().size());
        assertEquals("20 days paid leave.", result.finalOutput());
        assertEquals(1, result.taskResults().get(0).attempts());
    }

    @Test
    @DisplayName("TEST 2: Transient failure retries and succeeds on attempt 2")
    void testTransientFailureRetriesAndSucceeds() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentPlan plan = new AgentPlan("plan-retry-1", "Query", 1L, List.of(task));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        AtomicInteger callCount = new AtomicInteger(0);
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenAnswer(inv -> {
                    if (callCount.incrementAndGet() == 1) {
                        throw new RuntimeException("Vector index 503 unavailable");
                    }
                    return AgentResult.success(AgentType.SEARCH, "t1", "Recovered search output", List.of(), Map.of());
                });

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.COMPLETED, result.status());
        assertEquals(2, callCount.get());
        assertEquals(2, result.taskResults().get(0).attempts());
        assertEquals("Recovered search output", result.finalOutput());
    }

    @Test
    @DisplayName("TEST 3: Transient failure exhausts maximum attempts and marks task FAILED")
    void testTransientFailureExhaustsMaxAttempts() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentPlan plan = new AgentPlan("plan-retry-fail", "Query", 1L, List.of(task));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        AtomicInteger callCount = new AtomicInteger(0);
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenAnswer(inv -> {
                    callCount.incrementAndGet();
                    throw new RuntimeException("Connection 500 refused");
                });

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.FAILED, result.status());
        assertEquals(3, callCount.get());
        assertEquals(AgentTaskStatus.FAILED, result.taskResults().get(0).status());
        assertEquals(FailureCategory.TRANSIENT_FAILURE, result.taskResults().get(0).failureCategory());
    }

    @Test
    @DisplayName("TEST 4: Authorization failure is not retried")
    void testAuthorizationFailureNotRetried() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Unauthorized task", List.of());
        AgentPlan plan = new AgentPlan("plan-auth-fail", "Query", 1L, List.of(task));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        AtomicInteger callCount = new AtomicInteger(0);
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenAnswer(inv -> {
                    callCount.incrementAndGet();
                    throw new UnauthorizedAccessException("Forbidden resource access");
                });

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.FAILED, result.status());
        assertEquals(1, callCount.get());
        assertEquals(FailureCategory.AUTHORIZATION_FAILURE, result.taskResults().get(0).failureCategory());
    }

    @Test
    @DisplayName("TEST 5: Validation failure is not retried")
    void testValidationFailureNotRetried() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Malformed query", List.of());
        AgentPlan plan = new AgentPlan("plan-val-fail", "Query", 1L, List.of(task));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        AtomicInteger callCount = new AtomicInteger(0);
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenAnswer(inv -> {
                    callCount.incrementAndGet();
                    throw new IllegalArgumentException("Invalid task parameters");
                });

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.FAILED, result.status());
        assertEquals(1, callCount.get());
        assertEquals(FailureCategory.VALIDATION_FAILURE, result.taskResults().get(0).failureCategory());
    }

    @Test
    @DisplayName("TEST 6: Diamond dependency execution with upstream retry recovery")
    void testDiamondDependencyWithUpstreamRetry() {
        AgentTask task1 = new AgentTask("task-1", AgentTaskType.SEARCH, "Search Leave", List.of());
        AgentTask task2 = new AgentTask("task-2", AgentTaskType.SEARCH, "Search Remote", List.of());
        AgentTask task3 = new AgentTask("task-3", AgentTaskType.ANALYZE, "Compare Both", List.of("task-1", "task-2"));
        AgentTask task4 = new AgentTask("task-4", AgentTaskType.SYNTHESIZE, "Synthesize", List.of("task-3"));

        AgentPlan plan = new AgentPlan("plan-diamond", "Compare", 1L, List.of(task1, task2, task3, task4));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        AtomicInteger task1Calls = new AtomicInteger(0);
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenAnswer(inv -> {
                    AgentRequest req = inv.getArgument(0);
                    String tId = (String) req.parameters().get("taskId");
                    if ("task-1".equals(tId) && task1Calls.incrementAndGet() == 1) {
                        throw new RuntimeException("503 temporary error");
                    }
                    return AgentResult.success(AgentType.SEARCH, tId, "Search result " + tId, List.of(), Map.of());
                });

        when(analysisAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(AgentResult.success(AgentType.ANALYSIS, "ta", "Comparison result", List.of(), Map.of()));
        when(ragGenerationService.generateAnswer(anyString(), eq(1L), eq(5), eq(testUser)))
                .thenReturn(new RAGResponse("Final summary.", "Compare", 1L, List.of(), List.of(), true));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.COMPLETED, result.status());
        assertEquals(4, result.taskResults().size());
        assertEquals(2, result.taskResults().get(0).attempts());
        assertEquals("Final summary.", result.finalOutput());
    }

    @Test
    @DisplayName("TEST 7: Downstream dependency skipped when upstream retries fail")
    void testDependencyFailureSkipsDependentTask() {
        AgentTask task1 = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentTask task2 = new AgentTask("task-2", AgentTaskType.ANALYZE, "Analyze rules", List.of("task-1"));
        AgentPlan plan = new AgentPlan("plan-5", "Query", 1L, List.of(task1, task2));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenThrow(new RuntimeException("Vector index 500 error"));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.FAILED, result.status());
        assertEquals(AgentTaskStatus.FAILED, result.taskResults().get(0).status());
        assertEquals(AgentTaskStatus.SKIPPED, result.taskResults().get(1).status());
    }

    @Test
    @DisplayName("TEST 8: Invalid/circular plan is rejected before execution")
    void testInvalidPlanRejectedBeforeExecution() {
        AgentPlan invalidPlan = new AgentPlan("plan-invalid", "Query", 1L, List.of());
        when(planningService.validatePlan(invalidPlan)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> planExecutionService.executePlan(invalidPlan, testUser));
    }

    @Test
    @DisplayName("TEST 9: Unauthorized workspace access throws UnauthorizedAccessException")
    void testUnauthorizedWorkspaceThrowsException() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Search", List.of());
        AgentPlan plan = new AgentPlan("plan-unauth", "Query", 999L, List.of(task));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedAccessException.class, () -> planExecutionService.executePlan(plan, testUser));
    }
}
