package com.mycompany.transfersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeBreakdownDTO {
    private BigDecimal platformBaseFee;
    private BigDecimal platformExchangeProfit;
    private BigDecimal sendingBranchFee;
    private BigDecimal receivingBranchFee;
    private BigDecimal totalFee;
    private BigDecimal usdEquivalent;
    
    public FeeBreakdownDTO(BigDecimal platformBaseFee, BigDecimal platformExchangeProfit, 
                          BigDecimal sendingBranchFee, BigDecimal receivingBranchFee, BigDecimal usdEquivalent) {
        this.platformBaseFee = platformBaseFee;
        this.platformExchangeProfit = platformExchangeProfit;
        this.sendingBranchFee = sendingBranchFee;
        this.receivingBranchFee = receivingBranchFee;
        this.usdEquivalent = usdEquivalent;
        this.totalFee = platformBaseFee.add(platformExchangeProfit)
                                     .add(sendingBranchFee)
                                     .add(receivingBranchFee);
    }

    // Manual getters and setters for Lombok compatibility
    public BigDecimal getTotalFee() { return totalFee; }
    public void setTotalFee(BigDecimal totalFee) { this.totalFee = totalFee; }
    
    public BigDecimal getUsdEquivalent() { return usdEquivalent; }
    public void setUsdEquivalent(BigDecimal usdEquivalent) { this.usdEquivalent = usdEquivalent; }
    
    public BigDecimal getPlatformBaseFee() { return platformBaseFee; }
    public void setPlatformBaseFee(BigDecimal platformBaseFee) { this.platformBaseFee = platformBaseFee; }
    
    public BigDecimal getPlatformExchangeProfit() { return platformExchangeProfit; }
    public void setPlatformExchangeProfit(BigDecimal platformExchangeProfit) { this.platformExchangeProfit = platformExchangeProfit; }
    
    public BigDecimal getSendingBranchFee() { return sendingBranchFee; }
    public void setSendingBranchFee(BigDecimal sendingBranchFee) { this.sendingBranchFee = sendingBranchFee; }
    
    public BigDecimal getReceivingBranchFee() { return receivingBranchFee; }
    public void setReceivingBranchFee(BigDecimal receivingBranchFee) { this.receivingBranchFee = receivingBranchFee; }
}
