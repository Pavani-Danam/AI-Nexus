package com.ainexus.controller;

import com.ainexus.dto.RAGQueryRequest;
import com.ainexus.dto.RAGResponse;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.service.RAGGenerationService;
import com.ainexus.service.UserService;
import com.ainexus.service.WorkspaceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/rag", "/api/rag"})
public class RAGController {

    private static final Logger logger = LoggerFactory.getLogger(RAGController.class);

    private final RAGGenerationService ragGenerationService;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public RAGController(RAGGenerationService ragGenerationService,
                         UserService userService,
                         WorkspaceService workspaceService,
                         WorkspaceMemberRepository workspaceMemberRepository) {
        this.ragGenerationService = ragGenerationService;
        this.userService = userService;
        this.workspaceService = workspaceService;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @PostMapping("/query")
    public ResponseEntity<RAGResponse> queryRAG(
            @Valid @RequestBody RAGQueryRequest request,
            Authentication authentication) {

        User authenticatedUser = getAuthenticatedUser(authentication);
        Long targetWorkspaceId = resolveAuthorizedWorkspaceId(request.workspaceId(), authenticatedUser);

        logger.info("RAG query request received for workspace id: {} by user: {}", targetWorkspaceId, authenticatedUser.getUsername());

        RAGResponse response = ragGenerationService.generateAnswer(
                request.query(),
                targetWorkspaceId,
                request.topK(),
                authenticatedUser
        );

        return ResponseEntity.ok(response);
    }

    private Long resolveAuthorizedWorkspaceId(Long requestedWorkspaceId, User user) {
        if (requestedWorkspaceId != null) {
            // Validate user is owner or member of the requested workspace
            Workspace workspace = workspaceService.getWorkspaceById(requestedWorkspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + requestedWorkspaceId));

            boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
            boolean isMember = workspaceMemberRepository.findByWorkspaceAndUser(workspace, user).isPresent();

            if (!isOwner && !isMember) {
                throw new AccessDeniedException("Access denied to workspace id: " + requestedWorkspaceId);
            }
            return requestedWorkspaceId;
        }

        // Default to first workspace owned by or accessible to user
        List<Workspace> ownedWorkspaces = workspaceService.getWorkspacesByOwner(user);
        if (!ownedWorkspaces.isEmpty()) {
            return ownedWorkspaces.get(0).getId();
        }

        throw new ResourceNotFoundException("No active workspace found for user: " + user.getUsername());
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authentication required");
        }
        return userService.getUserByUsername(authentication.getName())
                .or(() -> userService.getUserByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
