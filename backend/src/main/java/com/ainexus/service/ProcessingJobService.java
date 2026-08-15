package com.ainexus.service;

import com.ainexus.entity.Document;
import com.ainexus.entity.ProcessingJob;
import com.ainexus.repository.ProcessingJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProcessingJobService {

    private final ProcessingJobRepository processingJobRepository;

    public ProcessingJobService(ProcessingJobRepository processingJobRepository) {
        this.processingJobRepository = processingJobRepository;
    }

    public ProcessingJob createJob(Document document, String jobType) {
        ProcessingJob job = ProcessingJob.builder()
                .document(document)
                .jobType(jobType != null ? jobType : "EMBEDDING")
                .status("PENDING")
                .build();
        return processingJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Optional<ProcessingJob> getJobById(Long id) {
        return processingJobRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ProcessingJob> getJobsByDocument(Document document) {
        return processingJobRepository.findByDocument(document);
    }

    @Transactional(readOnly = true)
    public List<ProcessingJob> getJobsByStatus(String status) {
        return processingJobRepository.findByStatus(status);
    }

    public ProcessingJob updateJobStatus(Long id, String status, String errorMessage) {
        return processingJobRepository.findById(id)
                .map(job -> {
                    job.setStatus(status);
                    job.setErrorMessage(errorMessage);
                    return processingJobRepository.save(job);
                })
                .orElseThrow(() -> new IllegalArgumentException("ProcessingJob not found with id: " + id));
    }
}
