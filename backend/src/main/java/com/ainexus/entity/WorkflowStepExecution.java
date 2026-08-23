package com.ainexus.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_step_executions")
public class WorkflowStepExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_execution_id", nullable = false)
    @JsonBackReference
    private WorkflowExecution workflowExecution;

    @Column(name = "step_key", nullable = false)
    private String stepKey;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false)
    private WorkflowStepType stepType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowExecutionStatus status = WorkflowExecutionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder = 0;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    public WorkflowStepExecution() {}

    public WorkflowStepExecution(String stepKey, String stepName, WorkflowStepType stepType, Integer executionOrder) {
        this.stepKey = stepKey;
        this.stepName = stepName;
        this.stepType = stepType;
        this.executionOrder = executionOrder != null ? executionOrder : 0;
        this.status = WorkflowExecutionStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkflowExecution getWorkflowExecution() {
        return workflowExecution;
    }

    public void setWorkflowExecution(WorkflowExecution workflowExecution) {
        this.workflowExecution = workflowExecution;
    }

    public String getStepKey() {
        return stepKey;
    }

    public void setStepKey(String stepKey) {
        this.stepKey = stepKey;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public WorkflowStepType getStepType() {
        return stepType;
    }

    public void setStepType(WorkflowStepType stepType) {
        this.stepType = stepType;
    }

    public WorkflowExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowExecutionStatus status) {
        this.status = status;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
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
}
