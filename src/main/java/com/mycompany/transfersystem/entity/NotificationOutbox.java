package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.NotificationOutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_outbox",
        indexes = {
                @Index(name = "idx_notification_outbox_poll", columnList = "status,next_attempt_at"),
                @Index(name = "idx_notification_outbox_entity", columnList = "entity_type,entity_id"),
                @Index(name = "idx_notification_outbox_correlation", columnList = "correlation_id"),
                @Index(name = "idx_notification_outbox_recipient_created", columnList = "recipient_user_id,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "recipient_role", length = 40)
    private String recipientRole;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(name = "phone_masked", length = 32)
    private String phoneMasked;

    @Column(name = "recipient_phone_e164", length = 32)
    private String recipientPhoneE164;

    @Column(name = "template_key", nullable = false, length = 120)
    private String templateKey;

    @Column(name = "language_code", nullable = false, length = 10)
    @Builder.Default
    private String languageCode = "ar";

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationOutboxStatus status = NotificationOutboxStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "correlation_id", length = 64, unique = true)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = Instant.now();
        }
    }
}
