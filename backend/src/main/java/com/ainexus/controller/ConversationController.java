package com.ainexus.controller;

import com.ainexus.dto.ConversationRequest;
import com.ainexus.dto.ConversationResponse;
import com.ainexus.entity.Conversation;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.service.ConversationService;
import com.ainexus.service.UserService;
import com.ainexus.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserService userService;
    private final WorkspaceService workspaceService;

    public ConversationController(ConversationService conversationService,
                                  UserService userService,
                                  WorkspaceService workspaceService) {
        this.conversationService = conversationService;
        this.userService = userService;
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(@Valid @RequestBody ConversationRequest request) {
        User user = userService.getUserById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Workspace workspace = null;
        if (request.getWorkspaceId() != null) {
            workspace = workspaceService.getWorkspaceById(request.getWorkspaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + request.getWorkspaceId()));
        }

        Conversation conversation = Conversation.builder()
                .title(request.getTitle())
                .user(user)
                .workspace(workspace)
                .build();

        Conversation created = conversationService.createConversation(conversation);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getConversationById(@PathVariable Long id) {
        Conversation conversation = conversationService.getConversationById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
        return ResponseEntity.ok(mapToResponse(conversation));
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getConversationsByUser(@RequestParam Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<ConversationResponse> list = conversationService.getConversationsByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }

    private ConversationResponse mapToResponse(Conversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .userId(conversation.getUser().getId())
                .workspaceId(conversation.getWorkspace() != null ? conversation.getWorkspace().getId() : null)
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}
