package com.ainexus.service;

import com.ainexus.dto.WorkflowRequest;
import com.ainexus.dto.WorkflowResponse;
import com.ainexus.dto.WorkflowStepRequest;
import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkflowRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.impl.WorkflowServiceImpl;
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
class WorkflowServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private User owner;
    private User intruder;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(10L);
        owner.setUsername("owner");

        intruder = new User();
        intruder.setId(20L);
        intruder.setUsername("intruder");

        testWorkspace = new Workspace();
        testWorkspace.setId(100L);
        testWorkspace.setName("Engineering");
        testWorkspace.setOwner(owner);
    }

    @Test
    @DisplayName("TEST 1: Create valid workflow with linear and multi-step dependencies")
    void testCreateValidWorkflow() {
        List<WorkflowStepRequest> steps = List.of(
                new WorkflowStepRequest("s1", "Search Docs", WorkflowStepType.SEARCH, "{\"query\":\"leave\"}", 1, List.of(), true),
                new WorkflowStepRequest("s2", "Analyze Findings", WorkflowStepType.ANALYZE, "{}", 2, List.of("s1"), true),
                new WorkflowStepRequest("s3", "Synthesize Report", WorkflowStepType.SYNTHESIZE, "{}", 3, List.of("s2"), true)
        );
        WorkflowRequest request = new WorkflowRequest("Leave Analysis Workflow", "Extract and summarize leave policy", 100L, WorkflowStatus.DRAFT, steps);

        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(workflowRepository.existsByWorkspaceIdAndNameIgnoreCase(100L, "Leave Analysis Workflow")).thenReturn(false);
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(inv -> {
            Workflow w = inv.getArgument(0);
            w.setId(1L);
            return w;
        });

        WorkflowResponse response = workflowService.createWorkflow(request, owner);

        assertNotNull(response);
        assertEquals("Leave Analysis Workflow", response.name());
        assertEquals(3, response.steps().size());
        assertEquals(WorkflowStatus.DRAFT, response.status());
        verify(workflowRepository, times(1)).save(any(Workflow.class));
    }

    @Test
    @DisplayName("TEST 2: Reject workflow with duplicate step keys")
    void testRejectDuplicateStepKeys() {
        List<WorkflowStepRequest> steps = List.of(
                new WorkflowStepRequest("step-dup", "Search Step 1", WorkflowStepType.SEARCH, "{}", 1, List.of(), true),
                new WorkflowStepRequest("step-dup", "Search Step 2", WorkflowStepType.SEARCH, "{}", 2, List.of(), true)
        );
        WorkflowRequest request = new WorkflowRequest("Duplicate Step Workflow", "Desc", 100L, WorkflowStatus.DRAFT, steps);

        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(workflowRepository.existsByWorkspaceIdAndNameIgnoreCase(100L, "Duplicate Step Workflow")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> workflowService.createWorkflow(request, owner));
        assertTrue(ex.getMessage().contains("Duplicate step key"));
    }

    @Test
    @DisplayName("TEST 3: Reject workflow with non-existent dependency")
    void testRejectNonExistentDependency() {
        List<WorkflowStepRequest> steps = List.of(
                new WorkflowStepRequest("s1", "Search Step", WorkflowStepType.SEARCH, "{}", 1, List.of("missing-step"), true)
        );
        WorkflowRequest request = new WorkflowRequest("Missing Dep Workflow", "Desc", 100L, WorkflowStatus.DRAFT, steps);

        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(workflowRepository.existsByWorkspaceIdAndNameIgnoreCase(100L, "Missing Dep Workflow")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> workflowService.createWorkflow(request, owner));
        assertTrue(ex.getMessage().contains("references non-existent dependency"));
    }

    @Test
    @DisplayName("TEST 4: Reject workflow with circular dependency")
    void testRejectCircularDependency() {
        List<WorkflowStepRequest> steps = List.of(
                new WorkflowStepRequest("s1", "Step 1", WorkflowStepType.SEARCH, "{}", 1, List.of("s2"), true),
                new WorkflowStepRequest("s2", "Step 2", WorkflowStepType.ANALYZE, "{}", 2, List.of("s1"), true)
        );
        WorkflowRequest request = new WorkflowRequest("Circular Workflow", "Desc", 100L, WorkflowStatus.DRAFT, steps);

        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(workflowRepository.existsByWorkspaceIdAndNameIgnoreCase(100L, "Circular Workflow")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> workflowService.createWorkflow(request, owner));
        assertTrue(ex.getMessage().contains("Circular dependency detected"));
    }

    @Test
    @DisplayName("TEST 5: Enforce workspace authorization boundary")
    void testWorkspaceAuthorizationEnforced() {
        WorkflowRequest request = new WorkflowRequest("Secret Workflow", "Desc", 100L, WorkflowStatus.DRAFT, List.of());
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));

        assertThrows(UnauthorizedAccessException.class, () -> workflowService.createWorkflow(request, intruder));
    }

    @Test
    @DisplayName("TEST 6: Update workflow and increment version")
    void testUpdateWorkflowIncrementsVersion() {
        Workflow existing = new Workflow("Old Name", "Old Desc", testWorkspace, owner);
        existing.setId(5L);
        existing.setVersion(1);

        WorkflowRequest updateReq = new WorkflowRequest("New Name", "New Desc", 100L, WorkflowStatus.ACTIVE, List.of());

        when(workflowRepository.findByIdWithSteps(5L)).thenReturn(Optional.of(existing));
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowResponse updated = workflowService.updateWorkflow(5L, updateReq, owner);

        assertNotNull(updated);
        assertEquals("New Name", updated.name());
        assertEquals(2, updated.version());
        assertEquals(WorkflowStatus.ACTIVE, updated.status());
    }

    @Test
    @DisplayName("TEST 7: Delete workflow succeeds for authorized workspace owner")
    void testDeleteWorkflow() {
        Workflow existing = new Workflow("To Delete", "Desc", testWorkspace, owner);
        existing.setId(7L);

        when(workflowRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));

        workflowService.deleteWorkflow(7L, owner);

        verify(workflowRepository, times(1)).delete(existing);
    }

    @Test
    @DisplayName("TEST 8: Reject duplicate workflow name in same workspace")
    void testRejectDuplicateWorkflowName() {
        WorkflowRequest request = new WorkflowRequest("Existing Workflow", "Desc", 100L, WorkflowStatus.DRAFT, List.of());
        when(workspaceRepository.findById(100L)).thenReturn(Optional.of(testWorkspace));
        when(workflowRepository.existsByWorkspaceIdAndNameIgnoreCase(100L, "Existing Workflow")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> workflowService.createWorkflow(request, owner));
    }
}
