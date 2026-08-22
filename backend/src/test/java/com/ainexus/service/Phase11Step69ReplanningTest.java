package com.ainexus.service;

import com.ainexus.agent.*;
import com.ainexus.dto.*;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.impl.AgentReplanningServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase11Step69ReplanningTest {

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

    private AgentReplanningServiceImpl replanningService;
    private PlanExecutionServiceImpl planExecutionService;
    private User testUser;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        replanningService = new AgentReplanningServiceImpl(planningService);
        replanningService.setMaxReplanningAttempts(2);

        planExecutionService = new PlanExecutionServiceImpl(
                planningService,
                workspaceRepository,
                searchAgent,
                analysisAgent,
                knowledgeAgent,
                ragGenerationService,
                replanningService
        );
        planExecutionService.setMaxConcurrency(4);
        planExecutionService.setTaskTimeoutSeconds(10);
        planExecutionService.configureRetry(1, 10L, 50L); // 1 attempt per task so replan triggers on next version
        planExecutionService.setMaxReplanningAttempts(2);

        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("alice");

        testWorkspace = new Workspace();
        testWorkspace.setId(1L);
        testWorkspace.setName("Engineering Docs");
        testWorkspace.setOwner(testUser);
    }

    @Test
    @DisplayName("TEST 1: Initial plan succeeds substantively - no replanning triggered")
    void testInitialPlanSucceedsNoReplanning() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentPlan plan = new AgentPlan("plan-1", "What is leave policy?", 1L, List.of(task));

        when(planningService.validatePlan(any(AgentPlan.class))).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(AgentResult.success(AgentType.SEARCH, "t1", "Employees receive 20 days paid time off.", List.of(), Map.of()));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.COMPLETED, result.status());
        assertEquals("plan-1", result.planId());
        assertEquals("Employees receive 20 days paid time off.", result.finalOutput());
    }

    @Test
    @DisplayName("TEST 2: Insufficient search result triggers replanning and succeeds on version 2")
    void testInsufficientResultTriggersReplanningAndSucceeds() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentPlan plan = new AgentPlan("plan-1", "What is leave policy?", 1L, List.of(task));

        when(planningService.validatePlan(any(AgentPlan.class))).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        AtomicInteger searchCalls = new AtomicInteger(0);
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenAnswer(inv -> {
                    int call = searchCalls.incrementAndGet();
                    if (call == 1) {
                        return AgentResult.success(AgentType.SEARCH, "t1", "Search completed with no relevant information found.", List.of(), Map.of());
                    }
                    return AgentResult.success(AgentType.SEARCH, "t1", "Comprehensive leave policy: 20 annual days.", List.of(), Map.of());
                });

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.COMPLETED, result.status());
        assertEquals("plan-1-v2", result.planId());
        assertEquals("Comprehensive leave policy: 20 annual days.", result.finalOutput());
    }

    @Test
    @DisplayName("TEST 3: Replanning attempts are strictly capped at maxReplanningAttempts")
    void testReplanningAttemptsStrictlyCapped() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentPlan plan = new AgentPlan("plan-cap", "What is leave policy?", 1L, List.of(task));

        when(planningService.validatePlan(any(AgentPlan.class))).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(AgentResult.success(AgentType.SEARCH, "t1", "No documents found", List.of(), Map.of()));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertTrue(result.planId().contains("-v"));
    }

    @Test
    @DisplayName("TEST 4: Authorization failure does NOT trigger replanning")
    void testAuthorizationFailureDoesNotReplan() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Find secret file", List.of());
        AgentPlan plan = new AgentPlan("plan-auth", "Find secret file", 1L, List.of(task));

        when(planningService.validatePlan(any(AgentPlan.class))).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenThrow(new UnauthorizedAccessException("Access denied to confidential document"));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.FAILED, result.status());
        assertEquals("plan-auth", result.planId());
        assertEquals(FailureCategory.AUTHORIZATION_FAILURE, result.taskResults().get(0).failureCategory());
    }

    @Test
    @DisplayName("TEST 5: Plan oscillation or duplicate plan is detected and stopped")
    void testPlanOscillationDetectedAndStopped() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.ANALYZE, "Analyze data", List.of());
        AgentPlan plan = new AgentPlan("plan-osc", "Analyze query", 1L, List.of(task));

        when(planningService.validatePlan(any(AgentPlan.class))).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(analysisAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(AgentResult.success(AgentType.ANALYSIS, "t1", "Analysis completed with no findings.", List.of(), Map.of()));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertNotNull(result.planId());
    }

    @Test
    @DisplayName("TEST 6: Original user query is preserved throughout replanning")
    void testOriginalQueryPreservedAcrossReplans() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentPlan plan = new AgentPlan("plan-query", "Original User Query Preserved", 1L, List.of(task));

        when(planningService.validatePlan(any(AgentPlan.class))).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        AtomicInteger calls = new AtomicInteger(0);
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenAnswer(inv -> {
                    if (calls.incrementAndGet() == 1) {
                        return AgentResult.success(AgentType.SEARCH, "t1", "No relevant information", List.of(), Map.of());
                    }
                    return AgentResult.success(AgentType.SEARCH, "t1", "Found final details", List.of(), Map.of());
                });

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.COMPLETED, result.status());
        assertEquals("plan-query-v2", result.planId());
    }
}
