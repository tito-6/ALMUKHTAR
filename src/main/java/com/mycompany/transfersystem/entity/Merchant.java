package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(length = 50)
    private String category;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(length = 512)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 5)
    private String country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "qr_code_data", length = 512)
    private String qrCodeData;

    @Column(name = "qr_code_image_path", length = 512)
    private String qrCodeImagePath;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "kyc_approved", nullable = false)
    private boolean kycApproved = false;

    @Column(name = "processing_fee_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal processingFeePct = new BigDecimal("1.5");

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
