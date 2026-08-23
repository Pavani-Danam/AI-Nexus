package com.ainexus.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflow_executions")
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_id", nullable = false)
    private User triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowExecutionStatus status = WorkflowExecutionStatus.PENDING;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime = LocalDateTime.now();

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "final_output", columnDefinition = "TEXT")
    private String finalOutput;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "workflowExecution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("executionOrder ASC")
    @JsonManagedReference
    private List<WorkflowStepExecution> stepExecutions = new ArrayList<>();

    public WorkflowExecution() {}

    public WorkflowExecution(Workflow workflow, Workspace workspace, User triggeredBy) {
        this.workflow = workflow;
        this.workspace = workspace;
        this.triggeredBy = triggeredBy;
        this.status = WorkflowExecutionStatus.PENDING;
        this.startTime = LocalDateTime.now();
    }

    public void addStepExecution(WorkflowStepExecution stepExecution) {
        stepExecutions.add(stepExecution);
        stepExecution.setWorkflowExecution(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public User getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(User triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public WorkflowExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowExecutionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getFinalOutput() {
        return finalOutput;
    }

    public void setFinalOutput(String finalOutput) {
        this.finalOutput = finalOutput;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<WorkflowStepExecution> getStepExecutions() {
        return stepExecutions;
    }

    public void setStepExecutions(List<WorkflowStepExecution> stepExecutions) {
        this.stepExecutions = stepExecutions != null ? stepExecutions : new ArrayList<>();
    }
}
