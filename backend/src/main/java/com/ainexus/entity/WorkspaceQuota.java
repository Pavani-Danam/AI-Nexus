package com.ainexus.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_quotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false, unique = true)
    private Workspace workspace;

    // --- Configurable Limits ---
    @Builder.Default
    @Column(nullable = false)
    private long maxAiRequests = 1000L;

    @Builder.Default
    @Column(nullable = false)
    private long maxTokens = 500000L;

    @Builder.Default
    @Column(nullable = false)
    private long maxDocumentProcessing = 100L;

    @Builder.Default
    @Column(nullable = false)
    private long maxEmbeddings = 2000L;

    @Builder.Default
    @Column(nullable = false)
    private long maxVectorOperations = 5000L;

    @Builder.Default
    @Column(nullable = false)
    private long maxWorkflowExecutions = 200L;

    @Builder.Default
    @Column(nullable = false)
    private long maxAgentExecutions = 300L;

    // --- Current Tracked Usage ---
    @Builder.Default
    @Column(nullable = false)
    private long usedAiRequests = 0L;

    @Builder.Default
    @Column(nullable = false)
    private long usedTokens = 0L;

    @Builder.Default
    @Column(nullable = false)
    private long usedDocumentProcessing = 0L;

    @Builder.Default
    @Column(nullable = false)
    private long usedEmbeddings = 0L;

    @Builder.Default
    @Column(nullable = false)
    private long usedVectorOperations = 0L;

    @Builder.Default
    @Column(nullable = false)
    private long usedWorkflowExecutions = 0L;

    @Builder.Default
    @Column(nullable = false)
    private long usedAgentExecutions = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
