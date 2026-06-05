package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private boolean inAppEnabled = true;

    @Column(name = "whatsapp_enabled", nullable = false)
    @Builder.Default
    private boolean whatsappEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = false;

    @Column(name = "marketing_enabled", nullable = false)
    @Builder.Default
    private boolean marketingEnabled = true;

    @Column(name = "transaction_alerts_enabled", nullable = false)
    @Builder.Default
    private boolean transactionAlertsEnabled = true;

    @Column(name = "security_alerts_enabled", nullable = false)
    @Builder.Default
    private boolean securityAlertsEnabled = true;

    @Column(name = "language_preference", length = 10)
    @Builder.Default
    private String languagePreference = "EN";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
