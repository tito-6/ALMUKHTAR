package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_trading_notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTradingNotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "order_in_app", nullable = false)
    @Builder.Default
    private boolean orderInApp = true;

    @Column(name = "order_whatsapp", nullable = false)
    @Builder.Default
    private boolean orderWhatsApp = false;

    @Column(name = "price_alert_in_app", nullable = false)
    @Builder.Default
    private boolean priceAlertInApp = true;

    @Column(name = "price_alert_whatsapp", nullable = false)
    @Builder.Default
    private boolean priceAlertWhatsApp = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
