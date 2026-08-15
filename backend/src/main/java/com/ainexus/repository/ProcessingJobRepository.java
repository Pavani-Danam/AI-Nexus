package com.ainexus.repository;

import com.ainexus.entity.Document;
import com.ainexus.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {
    List<ProcessingJob> findByDocument(Document document);
    List<ProcessingJob> findByStatus(String status);
}
