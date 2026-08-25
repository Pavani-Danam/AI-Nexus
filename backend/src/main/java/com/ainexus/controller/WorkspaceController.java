package com.ainexus.controller;

import com.ainexus.dto.WorkspaceRequest;
import com.ainexus.dto.WorkspaceResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.repository.WorkspaceRepository;
import com.ainexus.service.WorkspaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceController(WorkspaceService workspaceService, WorkspaceRepository workspaceRepository) {
        this.workspaceService = workspaceService;
        this.workspaceRepository = workspaceRepository;
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getUserWorkspaces(@AuthenticationPrincipal User currentUser) {
        List<Workspace> workspaces = (currentUser != null) ? workspaceRepository.findByOwnerId(currentUser.getId()) : List.of();
        List<WorkspaceResponse> responses = workspaces.stream().map(ws -> WorkspaceResponse.builder()
                .id(ws.getId())
                .name(ws.getName())
                .description(ws.getDescription())
                .createdAt(ws.getCreatedAt())
                .build()).collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @RequestBody WorkspaceRequest request,
            @AuthenticationPrincipal User currentUser) {
        Workspace ws = Workspace.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(currentUser)
                .build();
        Workspace created = workspaceService.createWorkspace(ws);
        WorkspaceResponse response = WorkspaceResponse.builder()
                .id(created.getId())
                .name(created.getName())
                .description(created.getDescription())
                .createdAt(created.getCreatedAt())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
