package com.ainexus.service;

import com.ainexus.exception.FileNotFoundException;
import com.ainexus.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootLocation;

    public FileStorageService(@Value("${app.storage.documents-dir:uploads/documents}") String documentsDir) {
        this.rootLocation = Paths.get(documentsDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage directory: " + this.rootLocation, e);
        }
    }

    public String storeFile(InputStream inputStream, String originalFilename, Long workspaceId) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new FileStorageException("Original filename cannot be empty");
        }

        String cleanedFilename = StringUtils.cleanPath(originalFilename);
        if (cleanedFilename.contains("..")) {
            throw new FileStorageException("Filename contains invalid path sequence: " + cleanedFilename);
        }

        String extension = "";
        int dotIndex = cleanedFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = cleanedFilename.substring(dotIndex);
        }

        String storedFilename = UUID.randomUUID() + extension;
        Path targetDir = (workspaceId != null) ? this.rootLocation.resolve(String.valueOf(workspaceId)).normalize() : this.rootLocation;

        try {
            Files.createDirectories(targetDir);
            Path destinationFile = targetDir.resolve(storedFilename).normalize();

            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new FileStorageException("Cannot store file outside current storage directory");
            }

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);

            return (workspaceId != null) ? workspaceId + "/" + storedFilename : storedFilename;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + cleanedFilename, e);
        }
    }

    public Resource loadFileAsResource(String relativeStoragePath) {
        try {
            Path filePath = this.rootLocation.resolve(relativeStoragePath).normalize();

            if (!filePath.startsWith(this.rootLocation)) {
                throw new FileStorageException("Access denied: path escapes storage directory");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundException("File not found: " + relativeStoragePath);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("File not found: " + relativeStoragePath, e);
        }
    }

    public boolean deleteFile(String relativeStoragePath) {
        if (relativeStoragePath == null || relativeStoragePath.trim().isEmpty()) {
            return false;
        }
        try {
            Path filePath = this.rootLocation.resolve(relativeStoragePath).normalize();
            if (!filePath.startsWith(this.rootLocation)) {
                throw new FileStorageException("Access denied: path escapes storage directory");
            }
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file: " + relativeStoragePath, e);
        }
    }

    public boolean fileExists(String relativeStoragePath) {
        if (relativeStoragePath == null || relativeStoragePath.trim().isEmpty()) {
            return false;
        }
        Path filePath = this.rootLocation.resolve(relativeStoragePath).normalize();
        return filePath.startsWith(this.rootLocation) && Files.exists(filePath);
    }

    public Path getRootLocation() {
        return this.rootLocation;
    }
}
