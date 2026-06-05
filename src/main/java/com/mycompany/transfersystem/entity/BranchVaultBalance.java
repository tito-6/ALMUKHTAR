package com.mycompany.transfersystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Physical vault (safe) balance by branch and currency, reconciled independently from drawer floats.
 */
@Entity
@Table(name = "branch_vault_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "currency"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchVaultBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "branch_id", insertable = false, updatable = false)
    private Long branchId;

    @Column(nullable = false, length = 10)
    private String currency;

    @Builder.Default
    @Column(name = "vault_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal vaultBalance = BigDecimal.ZERO;

    @Column(name = "last_reconciled_at")
    private LocalDateTime lastReconciledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
