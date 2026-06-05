package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "branch_cash_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchCashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private User cashier;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal amount;

    @Column(name = "available_after", nullable = false, precision = 20, scale = 4)
    private BigDecimal availableAfter;

    @Column(name = "reserved_after", nullable = false, precision = 20, scale = 4)
    private BigDecimal reservedAfter;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MovementType {
        CASH_COUNT_ADJUSTMENT,
        CASH_IN_RECEIVED,
        TRANSFER_PAYOUT_RESERVED,
        TRANSFER_PAYOUT_RELEASED,
        RESERVATION_CANCELLED,
        /** Ledger-backed change after manager-approved drawer adjustment */
        MANAGER_APPROVED_DRAWER_ADJUSTMENT,
        /** Inter-branch physical cash transfer (source branch) */
        INTER_BRANCH_CASH_SENT,
        /** Inter-branch physical cash transfer (destination branch) */
        INTER_BRANCH_CASH_RECEIVED
    }
}
