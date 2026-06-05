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

    /** Explicit cashier device identifier (may mirror device_id). */
    @Column(name = "cashier_device_id", length = 128)
    private String cashierDeviceId;

    @Column(name = "device_sequence_number")
    private Long deviceSequenceNumber;

    /** Canonical signed hash of decrypted payload (HMAC input). */
    @Column(name = "signed_payload_hash", length = 64)
    private String signedPayloadHash;

    @Column(name = "device_signature", length = 512)
    private String deviceSignature;

    /** When status is CONFLICT, records resolution lifecycle for managers. */
    @Column(name = "conflict_status", length = 40)
    private String conflictStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @Column(name = "payload_encrypted", nullable = false, length = 65535)
    private String payloadEncrypted;

    @Column(name = "payload_checksum", nullable = false, length = 64)
    private String payloadChecksum;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

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
