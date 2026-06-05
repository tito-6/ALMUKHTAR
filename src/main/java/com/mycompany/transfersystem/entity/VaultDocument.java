package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Entity
@Table(name = "vault_documents")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VaultDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;
    @Column(name = "document_type", length = 50)
    private String documentType;
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    @Column(name = "encryption_key_ref", length = 255)
    private String encryptionKeyRef;
    @Column(name = "uploaded_at")
    private Instant uploadedAt;
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
