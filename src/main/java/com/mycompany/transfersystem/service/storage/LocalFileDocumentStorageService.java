package com.mycompany.transfersystem.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Local filesystem implementation for KYC document storage (dev/testing).
 * Configure app.storage.kyc.path (default: ./data/kyc).
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileDocumentStorageService implements DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileDocumentStorageService.class);

    private final Path basePath;

    public LocalFileDocumentStorageService(
            @Value("${app.storage.kyc.path:./data/kyc}") String basePathStr) {
        this.basePath = Paths.get(basePathStr).toAbsolutePath();
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            log.warn("Could not create KYC storage directory: {}", basePath, e);
        }
    }

    @Override
    public String store(String applicationId, String docType, String filename, byte[] content) {
        try {
            Path dir = basePath.resolve(applicationId).resolve(docType);
            Files.createDirectories(dir);
            String safeName = filename != null && !filename.isBlank()
                    ? sanitize(filename)
                    : UUID.randomUUID().toString();
            Path file = dir.resolve(safeName);
            Files.write(file, content);
            return applicationId + "/" + docType + "/" + safeName;
        } catch (IOException e) {
            throw new com.mycompany.transfersystem.exception.StorageUnavailableException("Failed to store document: " + e.getMessage());
        }
    }

    @Override
    public byte[] retrieve(String fileReference) {
        try {
            Path file = basePath.resolve(fileReference);
            if (!Files.exists(file)) {
                return null;
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            log.warn("Failed to retrieve document: {}", fileReference, e);
            return null;
        }
    }

    @Override
    public InputStream retrieveStream(String fileReference) {
        byte[] bytes = retrieve(fileReference);
        return bytes != null ? new ByteArrayInputStream(bytes) : null;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
