package com.ainexus.controller;

import com.ainexus.entity.Citation;
import com.ainexus.entity.Message;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.service.CitationService;
import com.ainexus.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/citations")
public class CitationController {

    private final CitationService citationService;
    private final MessageService messageService;

    public CitationController(CitationService citationService, MessageService messageService) {
        this.citationService = citationService;
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<List<Citation>> getCitationsByMessage(@RequestParam Long messageId) {
        Message message = messageService.getMessageById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));
        return ResponseEntity.ok(citationService.getCitationsByMessage(message));
    }
}
