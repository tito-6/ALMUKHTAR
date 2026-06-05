package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "primary_color", length = 20)
    private String primaryColor;

    @Column(name = "secondary_color", length = 20)
    private String secondaryColor;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "monthly_saas_fee_usd", nullable = false, precision = 20, scale = 4)
    private BigDecimal monthlySaasFeeUsd = BigDecimal.ZERO;

    @Column(name = "per_transaction_royalty_bps", nullable = false)
    private int perTransactionRoyaltyBps = 0;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
