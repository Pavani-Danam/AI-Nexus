package com.ainexus.service;

import com.ainexus.entity.Conversation;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public Conversation createConversation(Conversation conversation) {
        if (conversation.getTitle() == null || conversation.getTitle().trim().isEmpty()) {
            conversation.setTitle("New Conversation");
        }
        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> getConversationById(Long id) {
        return conversationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getConversationsByUser(User user) {
        return conversationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getConversationsByWorkspace(Workspace workspace) {
        return conversationRepository.findByWorkspaceOrderByCreatedAtDesc(workspace);
    }

    public Conversation updateTitle(Long id, String title) {
        return conversationRepository.findById(id)
                .map(conv -> {
                    conv.setTitle(title);
                    return conversationRepository.save(conv);
                })
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found with id: " + id));
    }

    public void deleteConversation(Long id) {
        if (!conversationRepository.existsById(id)) {
            throw new IllegalArgumentException("Conversation not found with id: " + id);
        }
        conversationRepository.deleteById(id);
    }
}
