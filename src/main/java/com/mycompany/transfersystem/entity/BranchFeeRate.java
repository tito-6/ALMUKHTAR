package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "branch_fee_rates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_branch_fee_rates_branch", columnNames = {"branch_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchFeeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // $ per 1000 USD
    @Column(name = "sending_per_1000_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal sendingPerThousandUsd = new BigDecimal("1.00");

    // $ per 1000 USD (configurable 4.00 - 7.00)
    @Column(name = "receiving_per_1000_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal receivingPerThousandUsd = new BigDecimal("4.00");

    // Manual getters and setters for Lombok compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    
    public BigDecimal getSendingPerThousandUsd() { return sendingPerThousandUsd; }
    public void setSendingPerThousandUsd(BigDecimal sendingPerThousandUsd) { this.sendingPerThousandUsd = sendingPerThousandUsd; }
    
    public BigDecimal getReceivingPerThousandUsd() { return receivingPerThousandUsd; }
    public void setReceivingPerThousandUsd(BigDecimal receivingPerThousandUsd) { this.receivingPerThousandUsd = receivingPerThousandUsd; }
}
