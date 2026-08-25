package com.ainexus.controller;

import com.ainexus.dto.ChatRequest;
import com.ainexus.dto.ChatResponse;
import com.ainexus.dto.ConversationDto;
import com.ainexus.entity.User;
import com.ainexus.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(
            @RequestHeader(value = "X-Workspace-Id", required = false) Long workspaceIdHeader,
            @RequestParam(value = "workspaceId", required = false) Long workspaceIdParam,
            @AuthenticationPrincipal User currentUser) {
        Long workspaceId = workspaceIdHeader != null ? workspaceIdHeader : workspaceIdParam;
        List<ConversationDto> convs = chatService.getUserConversations(currentUser, workspaceId);
        return ResponseEntity.ok(convs);
    }

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-Workspace-Id", required = false) Long workspaceIdHeader,
            @AuthenticationPrincipal User currentUser) {
        Long workspaceId = workspaceIdHeader != null ? workspaceIdHeader : request.getWorkspaceId();
        ChatResponse response = chatService.processChat(request.getConversationId(), workspaceId, currentUser, request.getMessage());
        return ResponseEntity.ok(response);
    }
}
