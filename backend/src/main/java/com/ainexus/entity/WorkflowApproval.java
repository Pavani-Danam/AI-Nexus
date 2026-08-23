package com.ainexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_approvals", indexes = {
        @Index(name = "idx_approval_execution", columnList = "execution_id"),
        @Index(name = "idx_approval_workspace", columnList = "workspace_id"),
        @Index(name = "idx_approval_status", columnList = "status")
})
public class WorkflowApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private WorkflowExecution execution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "step_key", nullable = false)
    private String stepKey;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkflowApprovalStatus status = WorkflowApprovalStatus.PENDING;

    @Column(length = 1000)
    private String reason;

    @Column(name = "resolution_comment", length = 1000)
    private String resolutionComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public WorkflowApproval() {
    }

    public WorkflowApproval(WorkflowExecution execution, Workspace workspace, String stepKey,
                            String stepName, User requestedBy, String reason, LocalDateTime expiresAt) {
        this.execution = execution;
        this.workspace = workspace;
        this.stepKey = stepKey;
        this.stepName = stepName;
        this.requestedBy = requestedBy;
        this.reason = reason;
        this.expiresAt = expiresAt;
        this.status = WorkflowApprovalStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowExecution getExecution() { return execution; }
    public void setExecution(WorkflowExecution execution) { this.execution = execution; }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    public String getStepKey() { return stepKey; }
    public void setStepKey(String stepKey) { this.stepKey = stepKey; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }

    public User getApprover() { return approver; }
    public void setApprover(User approver) { this.approver = approver; }

    public WorkflowApprovalStatus getStatus() { return status; }
    public void setStatus(WorkflowApprovalStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getResolutionComment() { return resolutionComment; }
    public void setResolutionComment(String resolutionComment) { this.resolutionComment = resolutionComment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
