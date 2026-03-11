package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.KycDocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "kyc_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private WalletApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 30)
    private KycDocumentType docType;

    @Column(name = "file_reference", length = 512)
    private String fileReference;

    @Column(name = "upload_at", nullable = false)
    private Instant uploadAt;

    @Column(name = "ai_confidence_score", precision = 5, scale = 4)
    private BigDecimal aiConfidenceScore;

    @Column(name = "ai_flags", columnDefinition = "TEXT")
    private String aiFlags;

    @Column(name = "verified_by_human", nullable = false)
    @Builder.Default
    private Boolean verifiedByHuman = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
