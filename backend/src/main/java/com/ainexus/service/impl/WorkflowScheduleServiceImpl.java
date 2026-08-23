package com.ainexus.service.impl;

import com.ainexus.dto.WorkflowExecutionRequest;
import com.ainexus.dto.WorkflowScheduleRequest;
import com.ainexus.dto.WorkflowScheduleResponse;
import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkflowRepository;
import com.ainexus.repository.WorkflowScheduleRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.WorkflowExecutionService;
import com.ainexus.service.WorkflowScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Transactional
public class WorkflowScheduleServiceImpl implements WorkflowScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowScheduleServiceImpl.class);

    private final WorkflowScheduleRepository scheduleRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkflowExecutionService workflowExecutionService;

    public WorkflowScheduleServiceImpl(
            WorkflowScheduleRepository scheduleRepository,
            WorkflowRepository workflowRepository,
            WorkspaceRepository workspaceRepository,
            WorkflowExecutionService workflowExecutionService) {
        this.scheduleRepository = scheduleRepository;
        this.workflowRepository = workflowRepository;
        this.workspaceRepository = workspaceRepository;
        this.workflowExecutionService = workflowExecutionService;
    }

    @Override
    public WorkflowScheduleResponse createSchedule(Long workflowId, WorkflowScheduleRequest request, User user) {
        Objects.requireNonNull(workflowId, "Workflow ID must not be null");
        Objects.requireNonNull(request, "Schedule request must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        authorizeWorkspaceAccess(workflow.getWorkspace(), user);
        validateScheduleRequest(request);

        String timezone = resolveTimezone(request.timezone());
        ScheduleType type = request.scheduleType() != null ? request.scheduleType() : ScheduleType.RECURRING_CRON;

        WorkflowSchedule schedule = new WorkflowSchedule(
                workflow,
                workflow.getWorkspace(),
                type,
                request.cronExpression(),
                request.intervalSeconds(),
                timezone,
                request.inputQuery(),
                user
        );

        schedule.setNextExecutionAt(calculateNextExecutionTime(schedule, request.oneTimeExecutionAt()));
        WorkflowSchedule saved = scheduleRepository.save(schedule);

        logger.info("Created schedule id: {} for workflow id: {} in timezone: {} with next execution: {}",
                saved.getId(), workflowId, timezone, saved.getNextExecutionAt());

        return WorkflowScheduleResponse.fromEntity(saved);
    }

    @Override
    public WorkflowScheduleResponse updateSchedule(Long scheduleId, WorkflowScheduleRequest request, User user) {
        Objects.requireNonNull(scheduleId, "Schedule ID must not be null");
        Objects.requireNonNull(request, "Schedule request must not be null");
        Objects.requireNonNull(user, "User must not be null");

        WorkflowSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow schedule not found with ID: " + scheduleId));

        authorizeWorkspaceAccess(schedule.getWorkspace(), user);
        validateScheduleRequest(request);

        String timezone = resolveTimezone(request.timezone());
        schedule.setTimezone(timezone);
        schedule.setScheduleType(request.scheduleType() != null ? request.scheduleType() : schedule.getScheduleType());
        schedule.setCronExpression(request.cronExpression());
        schedule.setIntervalSeconds(request.intervalSeconds());
        schedule.setInputQuery(request.inputQuery());
        schedule.setUpdatedAt(LocalDateTime.now());

        if (schedule.isEnabled()) {
            schedule.setNextExecutionAt(calculateNextExecutionTime(schedule, request.oneTimeExecutionAt()));
        }

        WorkflowSchedule saved = scheduleRepository.save(schedule);
        logger.info("Updated schedule id: {} for workflow id: {}", saved.getId(), schedule.getWorkflow().getId());
        return WorkflowScheduleResponse.fromEntity(saved);
    }

    @Override
    public WorkflowScheduleResponse toggleSchedule(Long scheduleId, boolean enabled, User user) {
        Objects.requireNonNull(scheduleId, "Schedule ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        WorkflowSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow schedule not found with ID: " + scheduleId));

        authorizeWorkspaceAccess(schedule.getWorkspace(), user);
        schedule.setEnabled(enabled);
        schedule.setUpdatedAt(LocalDateTime.now());

        if (enabled) {
            schedule.setNextExecutionAt(calculateNextExecutionTime(schedule, null));
        } else {
            schedule.setNextExecutionAt(null);
        }

        WorkflowSchedule saved = scheduleRepository.save(schedule);
        logger.info("Toggled schedule id: {} enabled={}", saved.getId(), enabled);
        return WorkflowScheduleResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowScheduleResponse getScheduleById(Long scheduleId, User user) {
        Objects.requireNonNull(scheduleId, "Schedule ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        WorkflowSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow schedule not found with ID: " + scheduleId));

        authorizeWorkspaceAccess(schedule.getWorkspace(), user);
        return WorkflowScheduleResponse.fromEntity(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowScheduleResponse> getSchedulesByWorkflow(Long workflowId, User user) {
        Objects.requireNonNull(workflowId, "Workflow ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        authorizeWorkspaceAccess(workflow.getWorkspace(), user);
        List<WorkflowSchedule> list = scheduleRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
        return list.stream().map(WorkflowScheduleResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowScheduleResponse> getSchedulesByWorkspace(Long workspaceId, User user) {
        Objects.requireNonNull(workspaceId, "Workspace ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));

        authorizeWorkspaceAccess(workspace, user);
        List<WorkflowSchedule> list = scheduleRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        return list.stream().map(WorkflowScheduleResponse::fromEntity).toList();
    }

    @Override
    public void deleteSchedule(Long scheduleId, User user) {
        Objects.requireNonNull(scheduleId, "Schedule ID must not be null");
        Objects.requireNonNull(user, "User must not be null");

        WorkflowSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow schedule not found with ID: " + scheduleId));

        authorizeWorkspaceAccess(schedule.getWorkspace(), user);
        scheduleRepository.delete(schedule);
        logger.info("Deleted schedule id: {} by user: {}", scheduleId, user.getUsername());
    }

    @Scheduled(fixedDelay = 60000)
    @Override
    public void processDueSchedules() {
        LocalDateTime now = LocalDateTime.now();
        List<WorkflowSchedule> dueSchedules = scheduleRepository.findDueSchedules(now);
        if (dueSchedules.isEmpty()) return;

        logger.info("Processing {} due workflow schedules at {}", dueSchedules.size(), now);
        for (WorkflowSchedule schedule : dueSchedules) {
            try {
                // Prevent duplicate trigger: advance execution timestamp immediately
                LocalDateTime executedAt = LocalDateTime.now();
                schedule.setLastExecutionAt(executedAt);

                if (schedule.getScheduleType() == ScheduleType.ONE_TIME) {
                    schedule.setEnabled(false);
                    schedule.setNextExecutionAt(null);
                } else {
                    schedule.setNextExecutionAt(calculateNextExecutionTime(schedule, null));
                }
                scheduleRepository.save(schedule);

                // Trigger execution asynchronously/safely
                WorkflowExecutionRequest req = new WorkflowExecutionRequest(schedule.getInputQuery(), Map.of());
                workflowExecutionService.executeWorkflow(schedule.getWorkflow().getId(), req, schedule.getCreatedBy());

                logger.info("Triggered scheduled execution for schedule id: {}, workflow id: {}",
                        schedule.getId(), schedule.getWorkflow().getId());

            } catch (Exception e) {
                logger.error("Failed executing scheduled workflow for schedule id: {}", schedule.getId(), e);
            }
        }
    }

    private LocalDateTime calculateNextExecutionTime(WorkflowSchedule schedule, LocalDateTime oneTimeAt) {
        ZoneId zoneId = ZoneId.of(schedule.getTimezone());
        ZonedDateTime nowInZone = ZonedDateTime.now(zoneId);

        if (schedule.getScheduleType() == ScheduleType.ONE_TIME) {
            if (oneTimeAt == null) {
                throw new IllegalArgumentException("One-time execution requires a target execution timestamp.");
            }
            if (oneTimeAt.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("One-time execution timestamp cannot be in the past.");
            }
            return oneTimeAt;
        }

        if (schedule.getScheduleType() == ScheduleType.RECURRING_INTERVAL) {
            long seconds = (schedule.getIntervalSeconds() != null && schedule.getIntervalSeconds() > 0)
                    ? schedule.getIntervalSeconds()
                    : 3600;
            return LocalDateTime.now().plusSeconds(seconds);
        }

        // CRON Expression handling
        if (schedule.getCronExpression() == null || schedule.getCronExpression().isBlank()) {
            throw new IllegalArgumentException("Recurring cron schedule requires a valid cron expression.");
        }

        CronExpression cron = CronExpression.parse(schedule.getCronExpression());
        ZonedDateTime next = cron.next(nowInZone);
        if (next == null) {
            throw new IllegalArgumentException("Cron expression yielded no future execution dates.");
        }
        return next.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void validateScheduleRequest(WorkflowScheduleRequest request) {
        ScheduleType type = request.scheduleType() != null ? request.scheduleType() : ScheduleType.RECURRING_CRON;

        if (type == ScheduleType.RECURRING_CRON) {
            if (request.cronExpression() == null || request.cronExpression().isBlank()) {
                throw new IllegalArgumentException("Cron expression is required for cron-based recurring schedules.");
            }
            try {
                CronExpression.parse(request.cronExpression().trim());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid cron expression format: " + request.cronExpression());
            }
        } else if (type == ScheduleType.RECURRING_INTERVAL) {
            if (request.intervalSeconds() == null || request.intervalSeconds() <= 0) {
                throw new IllegalArgumentException("Interval seconds must be greater than zero.");
            }
        }
    }

    private String resolveTimezone(String tz) {
        if (tz == null || tz.isBlank()) return "UTC";
        try {
            return ZoneId.of(tz.trim()).getId();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone identifier: " + tz);
        }
    }

    private void authorizeWorkspaceAccess(Workspace workspace, User user) {
        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
        if (!isOwner) {
            logger.warn("[SECURITY] User {} unauthorized for workspace {}", user.getUsername(), workspace.getId());
            throw new UnauthorizedAccessException("You are not authorized to manage schedules in workspace ID: " + workspace.getId());
        }
    }
}
