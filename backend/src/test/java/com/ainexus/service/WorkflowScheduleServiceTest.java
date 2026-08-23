package com.ainexus.service;

import com.ainexus.dto.WorkflowScheduleRequest;
import com.ainexus.dto.WorkflowScheduleResponse;
import com.ainexus.entity.*;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkflowRepository;
import com.ainexus.repository.WorkflowScheduleRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.impl.WorkflowScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowScheduleServiceTest {

    @Mock
    private WorkflowScheduleRepository scheduleRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkflowExecutionService workflowExecutionService;

    @InjectMocks
    private WorkflowScheduleServiceImpl scheduleService;

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

        testWorkflow = new Workflow("Nightly Sync", "Sync data", testWorkspace, owner);
        testWorkflow.setId(1L);
    }

    @Test
    @DisplayName("TEST 1: Create recurring cron schedule with timezone successfully")
    void testCreateRecurringCronSchedule() {
        WorkflowScheduleRequest req = new WorkflowScheduleRequest(
                ScheduleType.RECURRING_CRON,
                "0 0 2 * * ?",
                null,
                "America/New_York",
                "Execute nightly sync",
                null
        );

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(scheduleRepository.save(any(WorkflowSchedule.class))).thenAnswer(inv -> {
            WorkflowSchedule s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });

        WorkflowScheduleResponse response = scheduleService.createSchedule(1L, req, owner);

        assertNotNull(response);
        assertEquals(50L, response.id());
        assertEquals("America/New_York", response.timezone());
        assertTrue(response.enabled());
        assertNotNull(response.nextExecutionAt());
    }

    @Test
    @DisplayName("TEST 2: Create one-time schedule in the future")
    void testCreateOneTimeSchedule() {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(2);
        WorkflowScheduleRequest req = new WorkflowScheduleRequest(
                ScheduleType.ONE_TIME,
                null,
                null,
                "UTC",
                "One off run",
                futureTime
        );

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(scheduleRepository.save(any(WorkflowSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowScheduleResponse response = scheduleService.createSchedule(1L, req, owner);

        assertNotNull(response);
        assertEquals(ScheduleType.ONE_TIME, response.scheduleType());
        assertEquals(futureTime, response.nextExecutionAt());
    }

    @Test
    @DisplayName("TEST 3: Reject schedule creation with invalid cron expression")
    void testRejectInvalidCronSchedule() {
        WorkflowScheduleRequest req = new WorkflowScheduleRequest(
                ScheduleType.RECURRING_CRON,
                "invalid-cron-string",
                null,
                "UTC",
                "Invalid run",
                null
        );

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> scheduleService.createSchedule(1L, req, owner));
        assertTrue(ex.getMessage().contains("Invalid cron expression"));
    }

    @Test
    @DisplayName("TEST 4: Reject schedule creation by unauthorized user")
    void testRejectUnauthorizedScheduleCreation() {
        WorkflowScheduleRequest req = new WorkflowScheduleRequest(
                ScheduleType.RECURRING_INTERVAL,
                null,
                3600L,
                "UTC",
                "Hourly run",
                null
        );

        when(workflowRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));

        assertThrows(UnauthorizedAccessException.class,
                () -> scheduleService.createSchedule(1L, req, intruder));
    }

    @Test
    @DisplayName("TEST 5: Toggle schedule enable and disable")
    void testToggleScheduleState() {
        WorkflowSchedule schedule = new WorkflowSchedule(
                testWorkflow, testWorkspace, ScheduleType.RECURRING_CRON, "0 0 12 * * ?", null, "UTC", null, owner);
        schedule.setId(10L);
        schedule.setEnabled(true);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(WorkflowSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        // Disable
        WorkflowScheduleResponse disabledResp = scheduleService.toggleSchedule(10L, false, owner);
        assertFalse(disabledResp.enabled());
        assertNull(disabledResp.nextExecutionAt());

        // Re-enable
        WorkflowScheduleResponse enabledResp = scheduleService.toggleSchedule(10L, true, owner);
        assertTrue(enabledResp.enabled());
        assertNotNull(enabledResp.nextExecutionAt());
    }

    @Test
    @DisplayName("TEST 6: Process due schedules and prevent duplicate execution")
    void testProcessDueSchedulesPreventsDuplicate() {
        WorkflowSchedule dueSchedule = new WorkflowSchedule(
                testWorkflow, testWorkspace, ScheduleType.RECURRING_INTERVAL, null, 1800L, "UTC", "Run sync", owner);
        dueSchedule.setId(99L);
        dueSchedule.setEnabled(true);
        dueSchedule.setNextExecutionAt(LocalDateTime.now().minusSeconds(10));

        when(scheduleRepository.findDueSchedules(any())).thenReturn(List.of(dueSchedule));
        when(scheduleRepository.save(any(WorkflowSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduleService.processDueSchedules();

        assertNotNull(dueSchedule.getLastExecutionAt());
        assertTrue(dueSchedule.getNextExecutionAt().isAfter(LocalDateTime.now()));
        verify(workflowExecutionService, times(1)).executeWorkflow(eq(1L), any(), eq(owner));
    }
}
