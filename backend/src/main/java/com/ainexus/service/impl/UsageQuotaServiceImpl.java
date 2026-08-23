package com.ainexus.service.impl;

import com.ainexus.dto.RecordUsageRequest;
import com.ainexus.dto.UpdateWorkspaceQuotaRequest;
import com.ainexus.dto.WorkspaceQuotaResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowAuditEventType;
import com.ainexus.entity.Workspace;
import com.ainexus.entity.WorkspaceQuota;
import com.ainexus.exception.QuotaExceededException;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.repository.WorkspaceQuotaRepository;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.UsageQuotaService;
import com.ainexus.service.WorkflowMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class UsageQuotaServiceImpl implements UsageQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(UsageQuotaServiceImpl.class);

    private final WorkspaceQuotaRepository quotaRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkflowMonitoringService monitoringService;

    public UsageQuotaServiceImpl(
            WorkspaceQuotaRepository quotaRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkflowMonitoringService monitoringService) {
        this.quotaRepository = quotaRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.monitoringService = monitoringService;
    }

    private void checkAdminAccess(User user) {
        Objects.requireNonNull(user, "User must not be null");
        String roleStr = user.getRole() != null ? user.getRole().name() : "";
        if (!roleStr.contains("ADMIN")) {
            logger.warn("[SECURITY] User '{}' attempted admin quota operation without ROLE_ADMIN", user.getUsername());
            throw new UnauthorizedAccessException("Forbidden: Administrator privileges required.");
        }
    }

    private WorkspaceQuota getOrCreateQuota(Long workspaceId) {
        return quotaRepository.findByWorkspace_Id(workspaceId)
                .orElseGet(() -> {
                    Workspace ws = workspaceRepository.findById(workspaceId)
                            .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + workspaceId));
                    WorkspaceQuota newQuota = WorkspaceQuota.builder()
                            .workspace(ws)
                            .build();
                    return quotaRepository.save(newQuota);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceQuotaResponse getWorkspaceQuota(Long workspaceId, User user) {
        Objects.requireNonNull(user, "User must not be null");
        String roleStr = user.getRole() != null ? user.getRole().name() : "";
        boolean isAdmin = roleStr.contains("ADMIN");

        if (!isAdmin) {
            boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId());
            if (!isMember) {
                throw new UnauthorizedAccessException("You are not a member of workspace ID: " + workspaceId);
            }
        }

        WorkspaceQuota quota = getOrCreateQuota(workspaceId);
        return WorkspaceQuotaResponse.fromEntity(quota);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceQuotaResponse> listAllWorkspaceQuotas(User adminUser) {
        checkAdminAccess(adminUser);
        return quotaRepository.findAll().stream()
                .map(WorkspaceQuotaResponse::fromEntity)
                .toList();
    }

    @Override
    public WorkspaceQuotaResponse updateWorkspaceQuota(Long workspaceId, UpdateWorkspaceQuotaRequest request, User adminUser) {
        checkAdminAccess(adminUser);
        Objects.requireNonNull(request, "Request must not be null");

        WorkspaceQuota quota = getOrCreateQuota(workspaceId);

        if (request.maxAiRequests() != null) quota.setMaxAiRequests(request.maxAiRequests());
        if (request.maxTokens() != null) quota.setMaxTokens(request.maxTokens());
        if (request.maxDocumentProcessing() != null) quota.setMaxDocumentProcessing(request.maxDocumentProcessing());
        if (request.maxEmbeddings() != null) quota.setMaxEmbeddings(request.maxEmbeddings());
        if (request.maxVectorOperations() != null) quota.setMaxVectorOperations(request.maxVectorOperations());
        if (request.maxWorkflowExecutions() != null) quota.setMaxWorkflowExecutions(request.maxWorkflowExecutions());
        if (request.maxAgentExecutions() != null) quota.setMaxAgentExecutions(request.maxAgentExecutions());

        WorkspaceQuota updated = quotaRepository.save(quota);

        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_UPDATED,
                null,
                workspaceId,
                null,
                adminUser.getUsername(),
                "Updated resource quota limits for workspace ID " + workspaceId
        );

        return WorkspaceQuotaResponse.fromEntity(updated);
    }

    @Override
    public WorkspaceQuotaResponse resetWorkspaceUsage(Long workspaceId, User adminUser) {
        checkAdminAccess(adminUser);

        WorkspaceQuota quota = getOrCreateQuota(workspaceId);
        quota.setUsedAiRequests(0L);
        quota.setUsedTokens(0L);
        quota.setUsedDocumentProcessing(0L);
        quota.setUsedEmbeddings(0L);
        quota.setUsedVectorOperations(0L);
        quota.setUsedWorkflowExecutions(0L);
        quota.setUsedAgentExecutions(0L);

        WorkspaceQuota updated = quotaRepository.save(quota);

        monitoringService.recordAuditEvent(
                WorkflowAuditEventType.WORKFLOW_UPDATED,
                null,
                workspaceId,
                null,
                adminUser.getUsername(),
                "Reset usage metrics to 0 for workspace ID " + workspaceId
        );

        return WorkspaceQuotaResponse.fromEntity(updated);
    }

    @Override
    public synchronized void checkAndRecordUsage(Long workspaceId, RecordUsageRequest usage) {
        if (usage == null) return;
        WorkspaceQuota quota = getOrCreateQuota(workspaceId);

        if (usage.aiRequests() > 0 && quota.getUsedAiRequests() + usage.aiRequests() > quota.getMaxAiRequests()) {
            throw new QuotaExceededException("AI Requests", quota.getUsedAiRequests(), quota.getMaxAiRequests());
        }
        if (usage.tokens() > 0 && quota.getUsedTokens() + usage.tokens() > quota.getMaxTokens()) {
            throw new QuotaExceededException("Tokens", quota.getUsedTokens(), quota.getMaxTokens());
        }
        if (usage.documentProcessing() > 0 && quota.getUsedDocumentProcessing() + usage.documentProcessing() > quota.getMaxDocumentProcessing()) {
            throw new QuotaExceededException("Document Processing", quota.getUsedDocumentProcessing(), quota.getMaxDocumentProcessing());
        }
        if (usage.embeddings() > 0 && quota.getUsedEmbeddings() + usage.embeddings() > quota.getMaxEmbeddings()) {
            throw new QuotaExceededException("Embeddings", quota.getUsedEmbeddings(), quota.getMaxEmbeddings());
        }
        if (usage.vectorOperations() > 0 && quota.getUsedVectorOperations() + usage.vectorOperations() > quota.getMaxVectorOperations()) {
            throw new QuotaExceededException("Vector Operations", quota.getUsedVectorOperations(), quota.getMaxVectorOperations());
        }
        if (usage.workflowExecutions() > 0 && quota.getUsedWorkflowExecutions() + usage.workflowExecutions() > quota.getMaxWorkflowExecutions()) {
            throw new QuotaExceededException("Workflow Executions", quota.getUsedWorkflowExecutions(), quota.getMaxWorkflowExecutions());
        }
        if (usage.agentExecutions() > 0 && quota.getUsedAgentExecutions() + usage.agentExecutions() > quota.getMaxAgentExecutions()) {
            throw new QuotaExceededException("Agent Executions", quota.getUsedAgentExecutions(), quota.getMaxAgentExecutions());
        }

        quota.setUsedAiRequests(quota.getUsedAiRequests() + Math.max(0, usage.aiRequests()));
        quota.setUsedTokens(quota.getUsedTokens() + Math.max(0, usage.tokens()));
        quota.setUsedDocumentProcessing(quota.getUsedDocumentProcessing() + Math.max(0, usage.documentProcessing()));
        quota.setUsedEmbeddings(quota.getUsedEmbeddings() + Math.max(0, usage.embeddings()));
        quota.setUsedVectorOperations(quota.getUsedVectorOperations() + Math.max(0, usage.vectorOperations()));
        quota.setUsedWorkflowExecutions(quota.getUsedWorkflowExecutions() + Math.max(0, usage.workflowExecutions()));
        quota.setUsedAgentExecutions(quota.getUsedAgentExecutions() + Math.max(0, usage.agentExecutions()));

        quotaRepository.save(quota);
    }

    @Override
    public synchronized void recordUsage(Long workspaceId, RecordUsageRequest usage) {
        if (usage == null) return;
        WorkspaceQuota quota = getOrCreateQuota(workspaceId);

        quota.setUsedAiRequests(quota.getUsedAiRequests() + Math.max(0, usage.aiRequests()));
        quota.setUsedTokens(quota.getUsedTokens() + Math.max(0, usage.tokens()));
        quota.setUsedDocumentProcessing(quota.getUsedDocumentProcessing() + Math.max(0, usage.documentProcessing()));
        quota.setUsedEmbeddings(quota.getUsedEmbeddings() + Math.max(0, usage.embeddings()));
        quota.setUsedVectorOperations(quota.getUsedVectorOperations() + Math.max(0, usage.vectorOperations()));
        quota.setUsedWorkflowExecutions(quota.getUsedWorkflowExecutions() + Math.max(0, usage.workflowExecutions()));
        quota.setUsedAgentExecutions(quota.getUsedAgentExecutions() + Math.max(0, usage.agentExecutions()));

        quotaRepository.save(quota);
    }
}
