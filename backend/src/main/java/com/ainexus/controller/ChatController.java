package com.ainexus.controller;

import com.ainexus.entity.User;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.service.ChatService;
import com.ainexus.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/chat", "/api/chat"})
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ChatService.ChatResponse> sendMessage(
            @RequestBody ChatRequest request,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.getUserByUsername(authentication.getName())
                .or(() -> userService.getUserByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        ChatService.ChatResponse response = chatService.processChat(
                request.conversationId(),
                request.workspaceId(),
                user,
                request.query()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatService.ConversationDto>> getConversations(
            @RequestParam("workspaceId") Long workspaceId,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.getUserByUsername(authentication.getName())
                .or(() -> userService.getUserByEmail(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        List<ChatService.ConversationDto> conversations = chatService.getUserConversations(user, workspaceId);
        return ResponseEntity.ok(conversations);
    }

    public record ChatRequest(
            Long conversationId,
            Long workspaceId,
            String query
    ) {}
}
