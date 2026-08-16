package com.ainexus.service;

import com.ainexus.entity.Document;
import com.ainexus.entity.DocumentStatus;
import com.ainexus.entity.User;
import com.ainexus.entity.Workspace;
import com.ainexus.exception.FileStorageException;
import com.ainexus.exception.ResourceNotFoundException;
import com.ainexus.exception.UnauthorizedAccessException;
import com.ainexus.repository.DocumentRepository;
import com.ainexus.repository.UserRepository;
import com.ainexus.repository.WorkspaceMemberRepository;
import com.ainexus.repository.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final FileStorageService fileStorageService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public DocumentService(DocumentRepository documentRepository,
                           FileStorageService fileStorageService,
                           WorkspaceRepository workspaceRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    public Document uploadDocument(MultipartFile file, Long workspaceId, User user) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty or missing");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Original filename cannot be empty");
        }

        String lowerFilename = originalFilename.toLowerCase();
        boolean isValidExtension = ALLOWED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
        if (!isValidExtension) {
            throw new IllegalArgumentException("Unsupported file type. Allowed formats: .pdf, .docx, .txt");
        }

        if (user == null || user.getId() == null) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        // Workspace Authorization Check: Must be Owner or Member
        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
        boolean isMember = workspaceMemberRepository.findByWorkspaceAndUser(workspace, user).isPresent();

        if (!isOwner && !isMember) {
            throw new UnauthorizedAccessException("User is not authorized to upload to this workspace");
        }

        String storagePath = null;
        try {
            storagePath = fileStorageService.storeFile(file.getInputStream(), originalFilename, workspaceId);

            Document document = Document.builder()
                    .fileName(originalFilename)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .storagePath(storagePath)
                    .status(DocumentStatus.UPLOADED)
                    .workspace(workspace)
                    .user(user)
                    .build();

            return documentRepository.save(document);
        } catch (IOException e) {
            if (storagePath != null) {
                fileStorageService.deleteFile(storagePath);
            }
            throw new FileStorageException("Failed to store physical file", e);
        } catch (Exception e) {
            if (storagePath != null) {
                fileStorageService.deleteFile(storagePath);
            }
            throw e;
        }
    }

    public Document uploadDocument(MultipartFile file, Long workspaceId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return uploadDocument(file, workspaceId, user);
    }

    @Transactional(readOnly = true)
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Document> getDocumentsByWorkspace(Long workspaceId, Pageable pageable) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));
        return documentRepository.findByWorkspace(workspace, pageable);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsByWorkspace(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));
        return documentRepository.findByWorkspace(workspace);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsByUser(User user) {
        return documentRepository.findByUser(user);
    }

    public Document updateDocumentStatus(Long id, DocumentStatus status) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        document.setStatus(status);
        return documentRepository.save(document);
    }

    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        fileStorageService.deleteFile(document.getStoragePath());
        documentRepository.delete(document);
    }
}
