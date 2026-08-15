package com.ainexus.controller;

import com.ainexus.dto.WorkspaceRequest;
import com.ainexus.dto.WorkspaceResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.service.UserService;
import com.ainexus.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final UserService userService;

    public WorkspaceController(WorkspaceService workspaceService, UserService userService) {
        this.workspaceService = workspaceService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(@Valid @RequestBody WorkspaceRequest request) {
        User owner = userService.getUserById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner user not found with id: " + request.getOwnerId()));

        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        Workspace created = workspaceService.createWorkspace(workspace);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspaceById(@PathVariable Long id) {
        Workspace workspace = workspaceService.getWorkspaceById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));
        return ResponseEntity.ok(mapToResponse(workspace));
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getWorkspacesByOwner(@RequestParam Long ownerId) {
        User owner = userService.getUserById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + ownerId));

        List<WorkspaceResponse> list = workspaceService.getWorkspacesByOwner(owner).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable Long id) {
        workspaceService.deleteWorkspace(id);
        return ResponseEntity.noContent().build();
    }

    private WorkspaceResponse mapToResponse(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .ownerId(workspace.getOwner().getId())
                .ownerUsername(workspace.getOwner().getUsername())
                .createdAt(workspace.getCreatedAt())
                .build();
    }
}
