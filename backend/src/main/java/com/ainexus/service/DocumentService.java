package com.ainexus.service;

import com.ainexus.entity.*;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.repository.DocumentRepository;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.repository.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DocumentService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".docx", ".txt");

    private final DocumentRepository documentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final FileStorageService fileStorageService;

    public DocumentService(DocumentRepository documentRepository,
                           WorkspaceRepository workspaceRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.fileStorageService = fileStorageService;
    }

    public Document uploadDocument(MultipartFile file, Long workspaceId, User user) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String lowerFilename = originalFilename.toLowerCase();
        boolean hasValidExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
        if (!hasValidExt) {
            throw new IllegalArgumentException("Unsupported file type. Allowed formats: .pdf, .docx, .txt");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
        boolean isMember = workspaceMemberRepository.findByWorkspaceAndUser(workspace, user).isPresent();

        if (!isOwner && !isMember) {
            throw new AccessDeniedException("User is not authorized to upload documents to this workspace");
        }

        String storedPath = null;
        try {
            storedPath = fileStorageService.storeFile(file.getInputStream(), originalFilename, workspace.getId());

            Document document = Document.builder()
                    .fileName(originalFilename)
                    .originalFilename(originalFilename)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .storagePath(storedPath)
                    .status(DocumentStatus.UPLOADED)
                    .workspace(workspace)
                    .user(user)
                    .build();

            return documentRepository.save(document);
        } catch (IOException e) {
            if (storedPath != null) {
                fileStorageService.deleteFile(storedPath);
            }
            throw new RuntimeException("Failed to read upload file stream", e);
        } catch (Exception e) {
            if (storedPath != null) {
                fileStorageService.deleteFile(storedPath);
            }
            throw e;
        }
    }

    public Document saveDocument(Document document) {
        if (document.getFileName() == null || document.getFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("Document filename cannot be empty");
        }
        if (document.getStoragePath() == null || document.getStoragePath().trim().isEmpty()) {
            throw new IllegalArgumentException("Document storage path cannot be empty");
        }
        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsByUser(User user) {
        return documentRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsByWorkspace(Workspace workspace) {
        return documentRepository.findByWorkspace(workspace);
    }

    @Transactional(readOnly = true)
    public Page<Document> getDocumentsByWorkspace(Long workspaceId, Pageable pageable) {
        return documentRepository.findByWorkspaceId(workspaceId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsByStatus(DocumentStatus status) {
        return documentRepository.findByStatus(status);
    }

    public Document updateDocumentStatus(Long id, DocumentStatus status) {
        return documentRepository.findById(id)
                .map(doc -> {
                    doc.setStatus(status);
                    return documentRepository.save(doc);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
    }

    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        if (document.getStoragePath() != null) {
            fileStorageService.deleteFile(document.getStoragePath());
        }
        documentRepository.delete(document);
    }
}
