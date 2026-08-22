package com.ainexus.service;

import com.ainexus.dto.AgentPlan;
import com.ainexus.dto.AgentTask;
import com.ainexus.dto.AgentTaskType;
import com.ainexus.entity.User;
import com.ainexus.service.impl.AgentPlanningServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentPlanningServiceImplTest {

    private AgentPlanningServiceImpl planningService;
    private User testUser;

    @BeforeEach
    void setUp() {
        planningService = new AgentPlanningServiceImpl();
        planningService.setMaxTasks(8);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("TEST 1: Simple query produces a single-task minimal plan")
    void testSimpleQueryProducesMinimalPlan() {
        AgentPlan plan = planningService.createPlan("What is the company leave policy?", 10L, testUser);

        assertNotNull(plan);
        assertEquals(10L, plan.workspaceId());
        assertFalse(plan.isComplex());
        assertEquals(1, plan.tasks().size());
        assertEquals(AgentTaskType.SEARCH, plan.tasks().get(0).type());
        assertTrue(planningService.validatePlan(plan));
    }

    @Test
    @DisplayName("TEST 2: Complex comparison query produces a multi-step plan with dependencies")
    void testComplexComparisonQueryProducesMultiStepPlan() {
        AgentPlan plan = planningService.createPlan("Compare the employee leave policy with the remote work policy and summarize the differences.", 10L, testUser);

        assertNotNull(plan);
        assertTrue(plan.isComplex());
        assertTrue(plan.tasks().size() > 1);
        assertTrue(planningService.validatePlan(plan));

        // Verify task sequence
        assertEquals("task-1", plan.tasks().get(0).id());
        assertTrue(plan.tasks().get(1).dependsOn().contains("task-1"));
    }

    @Test
    @DisplayName("TEST 3: Plan validation rejects duplicate task IDs")
    void testPlanValidationRejectsDuplicateTaskIds() {
        List<AgentTask> tasks = List.of(
                new AgentTask("task-1", AgentTaskType.SEARCH, "Search step 1", List.of()),
                new AgentTask("task-1", AgentTaskType.ANALYZE, "Analyze step 2", List.of("task-1"))
        );
        AgentPlan invalidPlan = new AgentPlan("plan-test", "Query", 10L, tasks);

        assertFalse(planningService.validatePlan(invalidPlan));
    }

    @Test
    @DisplayName("TEST 4: Plan validation rejects unknown/null task type")
    void testPlanValidationRejectsNullTaskType() {
        List<AgentTask> tasks = List.of(
                new AgentTask("task-1", null, "Search step", List.of())
        );
        AgentPlan invalidPlan = new AgentPlan("plan-test", "Query", 10L, tasks);

        assertFalse(planningService.validatePlan(invalidPlan));
    }

    @Test
    @DisplayName("TEST 5: Plan validation rejects missing/dangling dependency")
    void testPlanValidationRejectsMissingDependency() {
        List<AgentTask> tasks = List.of(
                new AgentTask("task-1", AgentTaskType.SEARCH, "Search step 1", List.of("non-existent-task"))
        );
        AgentPlan invalidPlan = new AgentPlan("plan-test", "Query", 10L, tasks);

        assertFalse(planningService.validatePlan(invalidPlan));
    }

    @Test
    @DisplayName("TEST 6: Plan validation rejects self-dependency")
    void testPlanValidationRejectsSelfDependency() {
        List<AgentTask> tasks = List.of(
                new AgentTask("task-1", AgentTaskType.SEARCH, "Search step 1", List.of("task-1"))
        );
        AgentPlan invalidPlan = new AgentPlan("plan-test", "Query", 10L, tasks);

        assertFalse(planningService.validatePlan(invalidPlan));
    }

    @Test
    @DisplayName("TEST 7: Plan validation rejects circular dependencies")
    void testPlanValidationRejectsCircularDependency() {
        List<AgentTask> tasks = List.of(
                new AgentTask("task-1", AgentTaskType.SEARCH, "Search step 1", List.of("task-2")),
                new AgentTask("task-2", AgentTaskType.ANALYZE, "Analyze step 2", List.of("task-1"))
        );
        AgentPlan invalidPlan = new AgentPlan("plan-test", "Query", 10L, tasks);

        assertFalse(planningService.validatePlan(invalidPlan));
    }

    @Test
    @DisplayName("TEST 8: Plan validation rejects plans exceeding maximum task count")
    void testPlanValidationRejectsExceedingMaxTasks() {
        planningService.setMaxTasks(2);

        List<AgentTask> tasks = List.of(
                new AgentTask("task-1", AgentTaskType.SEARCH, "Search 1", List.of()),
                new AgentTask("task-2", AgentTaskType.SEARCH, "Search 2", List.of("task-1")),
                new AgentTask("task-3", AgentTaskType.SYNTHESIZE, "Synthesize", List.of("task-2"))
        );
        AgentPlan oversizedPlan = new AgentPlan("plan-test", "Query", 10L, tasks);

        assertFalse(planningService.validatePlan(oversizedPlan));
    }

    @Test
    @DisplayName("TEST 9: Invalid query inputs throw IllegalArgumentException")
    void testInvalidQueryInputsThrowException() {
        assertThrows(IllegalArgumentException.class, () -> planningService.createPlan("", 10L, testUser));
        assertThrows(IllegalArgumentException.class, () -> planningService.createPlan(null, 10L, testUser));
        assertThrows(IllegalArgumentException.class, () -> planningService.createPlan("Query", null, testUser));
    }
}
