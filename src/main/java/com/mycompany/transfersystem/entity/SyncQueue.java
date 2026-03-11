package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "sync_queue")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @Column(name = "payload_encrypted", nullable = false, length = 65535)
    private String payloadEncrypted;

    @Column(name = "payload_checksum", nullable = false, length = 64)
    private String payloadChecksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus status = SyncStatus.PENDING;

    @Column(name = "offline_timestamp", nullable = false)
    private Instant offlineTimestamp;

    @Column(name = "synced_at")
    private Instant syncedAt;

    @Column(name = "conflict_reason", length = 65535)
    private String conflictReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
