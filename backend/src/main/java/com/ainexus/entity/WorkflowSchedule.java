package com.ainexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_schedules", indexes = {
        @Index(name = "idx_schedule_workflow", columnList = "workflow_id"),
        @Index(name = "idx_schedule_workspace", columnList = "workspace_id"),
        @Index(name = "idx_schedule_enabled_next", columnList = "enabled, next_execution_at")
})
public class WorkflowSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 30)
    private ScheduleType scheduleType = ScheduleType.RECURRING_CRON;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "interval_seconds")
    private Long intervalSeconds;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "UTC";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "next_execution_at")
    private LocalDateTime nextExecutionAt;

    @Column(name = "last_execution_at")
    private LocalDateTime lastExecutionAt;

    @Column(name = "input_query", length = 2000)
    private String inputQuery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public WorkflowSchedule() {
    }

    public WorkflowSchedule(Workflow workflow, Workspace workspace, ScheduleType scheduleType,
                            String cronExpression, Long intervalSeconds, String timezone,
                            String inputQuery, User createdBy) {
        this.workflow = workflow;
        this.workspace = workspace;
        this.scheduleType = scheduleType != null ? scheduleType : ScheduleType.RECURRING_CRON;
        this.cronExpression = cronExpression;
        this.intervalSeconds = intervalSeconds;
        this.timezone = (timezone != null && !timezone.isBlank()) ? timezone : "UTC";
        this.inputQuery = inputQuery;
        this.createdBy = createdBy;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Workflow getWorkflow() { return workflow; }
    public void setWorkflow(Workflow workflow) { this.workflow = workflow; }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public Long getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Long intervalSeconds) { this.intervalSeconds = intervalSeconds; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getNextExecutionAt() { return nextExecutionAt; }
    public void setNextExecutionAt(LocalDateTime nextExecutionAt) { this.nextExecutionAt = nextExecutionAt; }

    public LocalDateTime getLastExecutionAt() { return lastExecutionAt; }
    public void setLastExecutionAt(LocalDateTime lastExecutionAt) { this.lastExecutionAt = lastExecutionAt; }

    public String getInputQuery() { return inputQuery; }
    public void setInputQuery(String inputQuery) { this.inputQuery = inputQuery; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
