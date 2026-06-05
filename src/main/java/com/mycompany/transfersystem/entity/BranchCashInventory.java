package com.mycompany.transfersystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "branch_cash_inventory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "currency"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchCashInventory {

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
    @Column(name = "available_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "reserved_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "low_cash_threshold", nullable = false, precision = 20, scale = 4)
    private BigDecimal lowCashThreshold = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "high_cash_threshold", nullable = false, precision = 20, scale = 4)
    private BigDecimal highCashThreshold = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
