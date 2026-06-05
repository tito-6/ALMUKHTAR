package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_freeze_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountFreezeCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_case_id", nullable = false, unique = true, length = 40)
    private String publicCaseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "wallet_id")
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_category", nullable = false, length = 40)
    private CustomerVisibleCategory customerCategory;

    @Column(name = "internal_reason", length = 2000)
    private String internalReason;

    @Column(name = "customer_message", length = 2000)
    private String customerMessage;

    @Column(name = "sla_due_at")
    private LocalDateTime slaDueAt;

    @Column(name = "appeal_or_dispute_url", length = 1024)
    private String appealOrDisputeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_auditor_id")
    private User assignedAuditor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum CustomerVisibleCategory {
        COMPLIANCE_REVIEW,
        DISPUTE_RELATED,
        OPERATIONAL_HOLD,
        SECURITY_VERIFICATION,
        OTHER
    }

    public enum CaseStatus {
        OPEN,
        UNDER_REVIEW,
        RELEASED,
        ESCALATED
    }
}
