package com.ainexus.repository;

import com.ainexus.entity.Conversation;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserOrderByCreatedAtDesc(User user);
    List<Conversation> findByWorkspaceOrderByCreatedAtDesc(Workspace workspace);
}
