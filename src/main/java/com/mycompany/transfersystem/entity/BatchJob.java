package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "batch_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @Column(name = "batch_type", nullable = false, length = 20)
    private String batchType = "CSV_UPLOAD";

    @Column(length = 255)
    private String filename;

    @Column(name = "total_rows", nullable = false)
    private int totalRows = 0;

    @Column(name = "processed_rows", nullable = false)
    private int processedRows = 0;

    @Column(name = "success_count", nullable = false)
    private int successCount = 0;

    @Column(name = "failed_count", nullable = false)
    private int failedCount = 0;

    @Column(name = "total_amount_usd", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalAmountUsd = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    private String status = "VALIDATING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_wallet_id")
    private Wallet sourceWallet;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
