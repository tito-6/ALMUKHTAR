package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.NotificationChannel;
import com.mycompany.transfersystem.entity.enums.NotificationDeliveryStatus;
import com.mycompany.transfersystem.entity.enums.NotificationEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_delivery_logs", indexes = {
        @Index(name = "idx_ndl_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_ndl_entity", columnList = "entity_type,entity_id"),
        @Index(name = "idx_ndl_status_created", columnList = "status,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "phone_masked", nullable = false, length = 32)
    private String phoneMasked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private NotificationEventType eventType;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "template_key", nullable = false, length = 120)
    private String templateKey;

    @Column(nullable = false, length = 10)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationDeliveryStatus status;

    @Column(name = "provider_message_id", unique = true, length = 128)
    private String providerMessageId;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "failure_reason", length = 1024)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = NotificationDeliveryStatus.PENDING;
        }
    }
}
