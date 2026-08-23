package com.ainexus.service.impl;

import com.ainexus.dto.AdminDashboardSummaryResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.WorkflowExecution;
import com.ainexus.entity.WorkflowExecutionStatus;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.*;
import com.ainexus.service.AdminDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardServiceImpl.class);

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final DocumentRepository documentRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;

    public AdminDashboardServiceImpl(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            DocumentRepository documentRepository,
            WorkflowRepository workflowRepository,
            WorkflowExecutionRepository executionRepository) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.documentRepository = documentRepository;
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
    }

    @Override
    public AdminDashboardSummaryResponse getDashboardSummary(User requestingUser) {
        Objects.requireNonNull(requestingUser, "Requesting user must not be null");

        if (!isAdmin(requestingUser)) {
            logger.warn("[SECURITY] Access denied. Non-admin user '{}' attempted to access admin dashboard summary.",
                    requestingUser.getUsername());
            throw new UnauthorizedAccessException("Forbidden: Administrator privileges required.");
        }

        logger.info("Admin dashboard summary requested by admin user '{}'", requestingUser.getUsername());

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(User::isEnabled).count();
        long totalWorkspaces = workspaceRepository.count();
        long totalDocuments = documentRepository.count();
        long totalWorkflows = workflowRepository.count();

        List<WorkflowExecution> executions = executionRepository.findAll();
        long totalExecutions = executions.size();
        long successfulExecutions = executions.stream().filter(e -> e.getStatus() == WorkflowExecutionStatus.COMPLETED).count();
        long failedExecutions = executions.stream().filter(e -> e.getStatus() == WorkflowExecutionStatus.FAILED).count();

        long estimatedAiTokens = totalDocuments * 1500L + totalExecutions * 3200L;

        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> systemMetrics = new HashMap<>();
        systemMetrics.put("jvmTotalMemoryBytes", runtime.totalMemory());
        systemMetrics.put("jvmFreeMemoryBytes", runtime.freeMemory());
        systemMetrics.put("jvmMaxMemoryBytes", runtime.maxMemory());
        systemMetrics.put("availableProcessors", runtime.availableProcessors());
        systemMetrics.put("systemUptimeMs", System.currentTimeMillis());

        return new AdminDashboardSummaryResponse(
                totalUsers,
                activeUsers,
                totalWorkspaces,
                totalDocuments,
                totalWorkflows,
                totalExecutions,
                successfulExecutions,
                failedExecutions,
                estimatedAiTokens,
                "HEALTHY",
                systemMetrics
        );
    }

    @Override
    public boolean isAdmin(User user) {
        if (user == null) return false;
        String roleStr = user.getRole() != null ? user.getRole().toString().toUpperCase() : "";
        return roleStr.contains("ADMIN");
    }
}
