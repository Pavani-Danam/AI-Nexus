package com.ainexus.service;

import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.entity.WorkspaceMember;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository workspaceMemberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    public Workspace createWorkspace(Workspace workspace) {
        if (workspace.getName() == null || workspace.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Workspace name cannot be empty");
        }
        Workspace saved = workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(saved)
                .user(saved.getOwner())
                .role("OWNER")
                .build();
        workspaceMemberRepository.save(ownerMember);

        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Workspace> getWorkspaceById(Long id) {
        return workspaceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Workspace> getWorkspacesByOwner(User owner) {
        return workspaceRepository.findByOwner(owner);
    }

    public WorkspaceMember addMember(Workspace workspace, User user, String role) {
        return workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
                .orElseGet(() -> workspaceMemberRepository.save(
                        WorkspaceMember.builder()
                                .workspace(workspace)
                                .user(user)
                                .role(role != null ? role : "MEMBER")
                                .build()
                ));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMember> getMembers(Workspace workspace) {
        return workspaceMemberRepository.findByWorkspace(workspace);
    }

    public void deleteWorkspace(Long id) {
        if (!workspaceRepository.existsById(id)) {
            throw new IllegalArgumentException("Workspace not found with id: " + id);
        }
        workspaceRepository.deleteById(id);
    }
}
