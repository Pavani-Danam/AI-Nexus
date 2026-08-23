package com.ainexus.service;

import com.ainexus.dto.AdminUserResponse;
import com.ainexus.dto.ManageWorkspaceMembershipRequest;
import com.ainexus.dto.UpdateUserRoleRequest;
import com.ainexus.dto.UpdateUserStatusRequest;
import com.ainexus.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserResponse> listUsers(String query, Pageable pageable, User adminUser);

    AdminUserResponse getUserDetails(Long userId, User adminUser);

    AdminUserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request, User adminUser);

    AdminUserResponse updateUserRole(Long userId, UpdateUserRoleRequest request, User adminUser);

    AdminUserResponse assignWorkspaceMembership(Long userId, ManageWorkspaceMembershipRequest request, User adminUser);

    AdminUserResponse removeWorkspaceMembership(Long userId, Long workspaceId, User adminUser);
}
