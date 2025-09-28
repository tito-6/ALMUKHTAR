package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.mycompany.transfersystem.entity.enums.CommissionScope;

import java.math.BigDecimal;

@Entity
@Table(name = "commission_rates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_commission_rates_branch_scope", columnNames = {"branch_id", "commission_scope"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_scope", nullable = false)
    private CommissionScope commissionScope;

    // Rate value per 1000 USD equivalent
    @Column(name = "rate_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal rateValue;


    public CommissionRate(Branch branch, CommissionScope commissionScope, BigDecimal rateValue) {
        this.branch = branch;
        this.commissionScope = commissionScope;
        this.rateValue = rateValue;
    }

}
