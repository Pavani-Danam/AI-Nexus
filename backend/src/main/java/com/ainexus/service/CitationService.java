package com.ainexus.service;

import com.ainexus.entity.Citation;
import com.ainexus.entity.Message;
import com.ainexus.repository.CitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CitationService {

    private final CitationRepository citationRepository;

    public CitationService(CitationRepository citationRepository) {
        this.citationRepository = citationRepository;
    }

    public Citation createCitation(Citation citation) {
        return citationRepository.save(citation);
    }

    public List<Citation> createAllCitations(List<Citation> citations) {
        return citationRepository.saveAll(citations);
    }

    @Transactional(readOnly = true)
    public List<Citation> getCitationsByMessage(Message message) {
        return citationRepository.findByMessage(message);
    }
}
