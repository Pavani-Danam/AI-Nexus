package com.ainexus.service;

import com.ainexus.dto.RecordUsageRequest;
import com.ainexus.dto.UpdateWorkspaceQuotaRequest;
import com.ainexus.dto.WorkspaceQuotaResponse;
import com.ainexus.entity.User;

import java.util.List;

public interface UsageQuotaService {

    WorkspaceQuotaResponse getWorkspaceQuota(Long workspaceId, User user);

    List<WorkspaceQuotaResponse> listAllWorkspaceQuotas(User adminUser);

    WorkspaceQuotaResponse updateWorkspaceQuota(Long workspaceId, UpdateWorkspaceQuotaRequest request, User adminUser);

    WorkspaceQuotaResponse resetWorkspaceUsage(Long workspaceId, User adminUser);

    void checkAndRecordUsage(Long workspaceId, RecordUsageRequest usage);

    void recordUsage(Long workspaceId, RecordUsageRequest usage);
}
