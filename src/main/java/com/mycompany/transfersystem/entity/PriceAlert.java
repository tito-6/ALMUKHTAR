package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.PriceAlertKind;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_alerts", indexes = {
        @Index(name = "idx_price_alerts_user", columnList = "user_id"),
        @Index(name = "idx_price_alerts_symbol", columnList = "symbol")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private PriceAlertKind kind;

    @Column(name = "threshold_price", precision = 20, scale = 6)
    private BigDecimal thresholdPrice;

    /** Absolute percent threshold for move alerts (e.g. 5 means 5%). */
    @Column(name = "threshold_percent", precision = 10, scale = 4)
    private BigDecimal thresholdPercent;

    /** Last known regular-market volume when alert was created (for spike detection). */
    @Column(name = "baseline_volume")
    private Long baselineVolume;

    @Column(name = "reference_price", precision = 20, scale = 6)
    private BigDecimal referencePrice;

    @Column(name = "notify_in_app", nullable = false)
    @Builder.Default
    private boolean notifyInApp = true;

    @Column(name = "notify_whatsapp", nullable = false)
    @Builder.Default
    private boolean notifyWhatsApp = false;

    @Column(name = "triggered", nullable = false)
    @Builder.Default
    private boolean triggered = false;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "whatsapp_sent", nullable = false)
    @Builder.Default
    private boolean whatsappSent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
