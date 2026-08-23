package com.ainexus.service;

import com.ainexus.dto.AdminCreateWorkspaceRequest;
import com.ainexus.dto.AdminUpdateWorkspaceRequest;
import com.ainexus.dto.AdminWorkspaceDetailResponse;
import com.ainexus.dto.ManageWorkspaceMembershipRequest;
import com.ainexus.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminWorkspaceService {

    Page<AdminWorkspaceDetailResponse> listWorkspaces(String query, Pageable pageable, User adminUser);

    AdminWorkspaceDetailResponse getWorkspaceDetails(Long workspaceId, User adminUser);

    AdminWorkspaceDetailResponse createWorkspace(AdminCreateWorkspaceRequest request, User adminUser);

    AdminWorkspaceDetailResponse updateWorkspace(Long workspaceId, AdminUpdateWorkspaceRequest request, User adminUser);

    AdminWorkspaceDetailResponse addOrUpdateMember(Long workspaceId, ManageWorkspaceMembershipRequest request, Long targetUserId, User adminUser);

    AdminWorkspaceDetailResponse removeMember(Long workspaceId, Long targetUserId, User adminUser);
}
