package com.ainexus.repository;

import com.ainexus.entity.Citation;
import com.ainexus.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitationRepository extends JpaRepository<Citation, Long> {
    List<Citation> findByMessage(Message message);
}
