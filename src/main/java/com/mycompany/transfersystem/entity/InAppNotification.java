package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "in_app_notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InAppNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String body;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private NotificationType type = NotificationType.SYSTEM;
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    public enum NotificationType { PROMO, ALERT, SYSTEM, TRANSACTION }
    public enum DeliveryStatus { PENDING, DELIVERED, READ }
}
