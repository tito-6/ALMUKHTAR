package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bill_payment_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillPaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private BillProvider provider;

    @Column(name = "account_reference", nullable = false, length = 128)
    private String accountReference;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    private String currency = "USD";

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "payment_ref", length = 128)
    private String paymentRef;

    @Column(name = "platform_fee", nullable = false, precision = 20, scale = 4)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
