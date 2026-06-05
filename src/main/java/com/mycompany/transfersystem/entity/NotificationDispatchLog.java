package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notification_dispatch_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDispatchLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 512)
    private String idempotencyKey;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "template_key", nullable = false, length = 120)
    private String templateKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
