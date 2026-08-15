package com.ainexus.controller;

import com.ainexus.dto.MessageRequest;
import com.ainexus.dto.MessageResponse;
import com.ainexus.entity.Conversation;
import com.ainexus.entity.Message;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.service.ConversationService;
import com.ainexus.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;
    private final ConversationService conversationService;

    public MessageController(MessageService messageService, ConversationService conversationService) {
        this.messageService = messageService;
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(@Valid @RequestBody MessageRequest request) {
        Conversation conversation = conversationService.getConversationById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + request.getConversationId()));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(request.getSender())
                .content(request.getContent())
                .build();

        Message created = messageService.saveMessage(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessagesByConversation(@RequestParam Long conversationId) {
        Conversation conversation = conversationService.getConversationById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        List<MessageResponse> list = messageService.getMessagesByConversation(conversation).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private MessageResponse mapToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .sender(message.getSender())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
