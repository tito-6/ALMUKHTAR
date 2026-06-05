package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "merchant_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_user_id", nullable = false)
    private User payerUser;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    private String currency = "USD";

    @Column(length = 255)
    private String description;

    @Column(name = "platform_fee", nullable = false, precision = 20, scale = 4)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "merchant_net_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal merchantNetAmount;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(name = "qr_scan_ref", length = 128)
    private String qrScanRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
