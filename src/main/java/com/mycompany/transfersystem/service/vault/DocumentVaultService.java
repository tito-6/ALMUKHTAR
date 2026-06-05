package com.mycompany.transfersystem.service.vault;

import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.VaultDocument;
import com.mycompany.transfersystem.exception.ConditionNotMetException;
import com.mycompany.transfersystem.exception.FileTooLargeException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.repository.VaultDocumentRepository;
import com.mycompany.transfersystem.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentVaultService {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;
    private final VaultDocumentRepository vaultDocumentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public VaultDocument uploadDocument(Long userId, MultipartFile file, String documentType) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileTooLargeException("File exceeds 50MB limit");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        String storagePath = "vault/" + userId + "/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
        VaultDocument doc = VaultDocument.builder()
                .ownerUser(user)
                .fileName(file.getOriginalFilename())
                .storagePath(storagePath)
                .documentType(documentType)
                .fileSizeBytes(file.getSize())
                .encryptionKeyRef("AES-256-SSE")
                .uploadedAt(Instant.now())
                .build();
        doc = vaultDocumentRepository.save(doc);
        auditService.log("VAULT_DOCUMENT_UPLOADED", "VAULT_DOCUMENT", doc.getId(), documentType, user);
        return doc;
    }

    @Transactional(readOnly = true)
    public String downloadDocument(Long documentId, Long requesterId) {
        VaultDocument doc = vaultDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (!doc.getOwnerUser().getId().equals(requesterId)) {
            throw new ConditionNotMetException("Access denied: not the document owner");
        }
        return doc.getStoragePath();
    }

    @Transactional
    public void deleteDocument(Long documentId, Long requesterId) {
        VaultDocument doc = vaultDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (!doc.getOwnerUser().getId().equals(requesterId)) {
            throw new ConditionNotMetException("Access denied: not the document owner");
        }
        vaultDocumentRepository.delete(doc);
        auditService.log("VAULT_DOCUMENT_DELETED", "VAULT_DOCUMENT", documentId, null, doc.getOwnerUser());
    }

    @Transactional(readOnly = true)
    public List<VaultDocument> listDocuments(Long userId) {
        return vaultDocumentRepository.findByOwnerUserId(userId);
    }
}
