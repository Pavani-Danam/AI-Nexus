package com.ainexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_audit_events", indexes = {
        @Index(name = "idx_audit_workflow", columnList = "workflow_id"),
        @Index(name = "idx_audit_workspace", columnList = "workspace_id"),
        @Index(name = "idx_audit_execution", columnList = "execution_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class WorkflowAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private WorkflowAuditEventType eventType;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "execution_id")
    private Long executionId;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public WorkflowAuditEvent() {}

    public WorkflowAuditEvent(WorkflowAuditEventType eventType, Long workflowId, Long workspaceId,
                              Long executionId, String actorUsername, String description) {
        this.eventType = eventType;
        this.workflowId = workflowId;
        this.workspaceId = workspaceId;
        this.executionId = executionId;
        this.actorUsername = actorUsername;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowAuditEventType getEventType() { return eventType; }
    public void setEventType(WorkflowAuditEventType eventType) { this.eventType = eventType; }

    public Long getWorkflowId() { return workflowId; }
    public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }

    public Long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(Long workspaceId) { this.workspaceId = workspaceId; }

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
