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
    }

    @Test
    @DisplayName("TEST 2: Two independent tasks execute successfully")
    void testTwoIndependentTasksExecute() {
        AgentTask task1 = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentTask task2 = new AgentTask("task-2", AgentTaskType.SEARCH, "Find remote work policy", List.of());
        AgentPlan plan = new AgentPlan("plan-2", "Query", 1L, List.of(task1, task2));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(AgentResult.success(AgentType.SEARCH, "t2", "Policy details found.", List.of(), Map.of()));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.COMPLETED, result.status());
        assertEquals(2, result.taskResults().size());
        assertEquals(AgentTaskStatus.COMPLETED, result.taskResults().get(0).status());
        assertEquals(AgentTaskStatus.COMPLETED, result.taskResults().get(1).status());
    }

    @Test
    @DisplayName("TEST 3: Dependent task receives previous task output and executes in order")
    void testDependentTasksExecuteInOrder() {
        AgentTask task1 = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentTask task2 = new AgentTask("task-2", AgentTaskType.ANALYZE, "Analyze policy rules", List.of("task-1"));
        AgentPlan plan = new AgentPlan("plan-3", "Query", 1L, List.of(task1, task2));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(AgentResult.success(AgentType.SEARCH, "t3a", "Raw leave details", List.of(), Map.of()));
        when(analysisAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(AgentResult.success(AgentType.ANALYSIS, "t3b", "Analysis summary: 20 days off.", List.of(), Map.of()));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.COMPLETED, result.status());
        assertEquals("Analysis summary: 20 days off.", result.finalOutput());
    }

    @Test
    @DisplayName("TEST 4: Three-level dependency chain (SEARCH -> ANALYZE -> SYNTHESIZE)")
    void testThreeLevelDependencyChain() {
        AgentTask task1 = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentTask task2 = new AgentTask("task-2", AgentTaskType.ANALYZE, "Extract rules", List.of("task-1"));
        AgentTask task3 = new AgentTask("task-3", AgentTaskType.SYNTHESIZE, "Synthesize into final answer", List.of("task-2"));
        AgentPlan plan = new AgentPlan("plan-4", "Query", 1L, List.of(task1, task2, task3));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(AgentResult.success(AgentType.SEARCH, "t4a", "Raw facts", List.of(), Map.of()));
        when(analysisAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenReturn(AgentResult.success(AgentType.ANALYSIS, "t4b", "Structured facts", List.of(), Map.of()));
        when(ragGenerationService.generateAnswer(anyString(), eq(1L), eq(5), eq(testUser)))
                .thenReturn(new RAGResponse("Final synthesized report.", "Query", 1L, List.of(), List.of(), true));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.COMPLETED, result.status());
        assertEquals("Final synthesized report.", result.finalOutput());
    }

    @Test
    @DisplayName("TEST 5: Upstream dependency failure marks dependent task as SKIPPED")
    void testDependencyFailureSkipsDependentTask() {
        AgentTask task1 = new AgentTask("task-1", AgentTaskType.SEARCH, "Find leave policy", List.of());
        AgentTask task2 = new AgentTask("task-2", AgentTaskType.ANALYZE, "Analyze rules", List.of("task-1"));
        AgentPlan plan = new AgentPlan("plan-5", "Query", 1L, List.of(task1, task2));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenThrow(new RuntimeException("Vector index unavailable"));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.FAILED, result.status());
        assertEquals(AgentTaskStatus.FAILED, result.taskResults().get(0).status());
        assertEquals(AgentTaskStatus.SKIPPED, result.taskResults().get(1).status());
    }

    @Test
    @DisplayName("TEST 6: Independent task executes even if another task fails")
    void testIndependentTaskExecutesWhenAnotherFails() {
        AgentTask task1 = new AgentTask("task-1", AgentTaskType.SEARCH, "Search leave policy", List.of());
        AgentTask task2 = new AgentTask("task-2", AgentTaskType.SEARCH, "Search remote work policy", List.of());
        AgentPlan plan = new AgentPlan("plan-6", "Query", 1L, List.of(task1, task2));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        when(searchAgent.execute(any(AgentRequest.class), any(AgentContext.class)))
                .thenThrow(new RuntimeException("Error in task 1"))
                .thenReturn(AgentResult.success(AgentType.SEARCH, "t6", "Remote policy retrieved", List.of(), Map.of()));

        AgentExecutionResult result = planExecutionService.executePlan(plan, testUser);

        assertNotNull(result);
        assertEquals(PlanExecutionStatus.PARTIALLY_COMPLETED, result.status());
        assertEquals(AgentTaskStatus.FAILED, result.taskResults().get(0).status());
        assertEquals(AgentTaskStatus.COMPLETED, result.taskResults().get(1).status());
    }

    @Test
    @DisplayName("TEST 7: Invalid/circular plan is rejected before execution")
    void testInvalidPlanRejectedBeforeExecution() {
        AgentPlan invalidPlan = new AgentPlan("plan-invalid", "Query", 1L, List.of());
        when(planningService.validatePlan(invalidPlan)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> planExecutionService.executePlan(invalidPlan, testUser));
    }

    @Test
    @DisplayName("TEST 8: Unauthorized workspace access throws UnauthorizedAccessException")
    void testUnauthorizedWorkspaceThrowsException() {
        AgentTask task = new AgentTask("task-1", AgentTaskType.SEARCH, "Search", List.of());
        AgentPlan plan = new AgentPlan("plan-unauth", "Query", 999L, List.of(task));

        when(planningService.validatePlan(plan)).thenReturn(true);
        when(workspaceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedAccessException.class, () -> planExecutionService.executePlan(plan, testUser));
    }
}
