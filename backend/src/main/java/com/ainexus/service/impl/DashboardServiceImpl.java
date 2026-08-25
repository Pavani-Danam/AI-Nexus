package com.ainexus.service.impl;

import com.ainexus.dto.DashboardSummaryResponse;
import com.ainexus.entity.*;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.*;
import com.ainexus.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final DocumentRepository documentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ConversationRepository conversationRepository;
    private final WorkflowExecutionRepository executionRepository;

    public DashboardServiceImpl(DocumentRepository documentRepository,
                                WorkspaceRepository workspaceRepository,
                                ConversationRepository conversationRepository,
                                WorkflowExecutionRepository executionRepository) {
        this.documentRepository = documentRepository;
        this.workspaceRepository = workspaceRepository;
        this.conversationRepository = conversationRepository;
        this.executionRepository = executionRepository;
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary(Long workspaceId, User user) {
        if (workspaceId != null) {
            Workspace workspace = workspaceRepository.findById(workspaceId)
                    .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
            
            if (user != null && workspace.getOwner() != null && !workspace.getOwner().getId().equals(user.getId())) {
                throw new UnauthorizedAccessException("Unauthorized access to workspace: " + workspaceId);
            }

            List<Document> docs = documentRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
            long indexed = docs.stream().filter(d -> d.getStatus() == DocumentStatus.INDEXED).count();
            long processing = docs.stream().filter(d -> d.getStatus() == DocumentStatus.PROCESSING).count();
            long failed = docs.stream().filter(d -> d.getStatus() == DocumentStatus.FAILED).count();
            long convs = conversationRepository.countByWorkspaceId(workspaceId);
            long execs = executionRepository.findByWorkspaceIdOrderByStartTimeDesc(workspaceId).size();

            return new DashboardSummaryResponse(
                    docs.size(),
                    indexed,
                    processing,
                    failed,
                    1L,
                    convs,
                    execs,
                    indexed * 12L
            );
        }

        long totalWorkspaces = user != null ? workspaceRepository.findByOwnerId(user.getId()).size() : 0L;
        return new DashboardSummaryResponse(0L, 0L, 0L, 0L, totalWorkspaces, 0L, 0L, 0L);
    }
}
