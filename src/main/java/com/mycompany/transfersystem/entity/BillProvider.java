package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bill_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(length = 5)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(name = "api_integration", nullable = false, length = 20)
    private String apiIntegration = "MANUAL";

    @Column(name = "api_endpoint", length = 512)
    private String apiEndpoint;

    @Column(name = "api_key_ref", length = 128)
    private String apiKeyRef;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "processing_fee_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal processingFeePct = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
