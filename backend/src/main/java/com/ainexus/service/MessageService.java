package com.ainexus.service;

import com.ainexus.entity.Conversation;
import com.ainexus.entity.Message;
import com.ainexus.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message saveMessage(Message message) {
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (message.getSender() == null || message.getSender().trim().isEmpty()) {
            throw new IllegalArgumentException("Message sender cannot be empty");
        }
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Optional<Message> getMessageById(Long id) {
        return messageRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByConversation(Conversation conversation) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }
}
