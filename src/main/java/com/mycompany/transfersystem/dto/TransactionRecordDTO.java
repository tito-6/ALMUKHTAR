package com.mycompany.transfersystem.dto;

import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecordDTO {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long fundId;
    
    // Transaction amounts
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    private BigDecimal totalFees;
    
    // Currency information
    private String sourceCurrency;
    private String destinationCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal usdEquivalent;
    
    // Fee breakdown
    private BigDecimal platformBaseFee;
    private BigDecimal platformExchangeProfit;
    private BigDecimal sendingBranchFee;
    private BigDecimal receivingBranchFee;
    
    // Branch information
    private Long senderBranchId;
    private Long receiverBranchId;
    private String senderBranchName;
    private String receiverBranchName;
    
    // Transaction status and timing
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Fund routing information
    private BigDecimal platformFundCredit;
    private BigDecimal senderBranchFundCredit;
    private BigDecimal receiverBranchFundCredit;
    private BigDecimal interBranchDebt;
    
    // Security information
    private String releasePasscode;

    // Manual getters and setters for Lombok compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    
    public Long getFundId() { return fundId; }
    public void setFundId(Long fundId) { this.fundId = fundId; }
    
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    
    public String getSourceCurrency() { return sourceCurrency; }
    public void setSourceCurrency(String sourceCurrency) { this.sourceCurrency = sourceCurrency; }
    
    public String getDestinationCurrency() { return destinationCurrency; }
    public void setDestinationCurrency(String destinationCurrency) { this.destinationCurrency = destinationCurrency; }
    
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }
    
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
    
    public Long getSenderBranchId() { return senderBranchId; }
    public void setSenderBranchId(Long senderBranchId) { this.senderBranchId = senderBranchId; }
    
    public Long getReceiverBranchId() { return receiverBranchId; }
    public void setReceiverBranchId(Long receiverBranchId) { this.receiverBranchId = receiverBranchId; }
    
    public BigDecimal getPlatformFundCredit() { return platformFundCredit; }
    public void setPlatformFundCredit(BigDecimal platformFundCredit) { this.platformFundCredit = platformFundCredit; }
    
    public BigDecimal getSenderBranchFundCredit() { return senderBranchFundCredit; }
    public void setSenderBranchFundCredit(BigDecimal senderBranchFundCredit) { this.senderBranchFundCredit = senderBranchFundCredit; }
    
    public BigDecimal getReceiverBranchFundCredit() { return receiverBranchFundCredit; }
    public void setReceiverBranchFundCredit(BigDecimal receiverBranchFundCredit) { this.receiverBranchFundCredit = receiverBranchFundCredit; }
    
    public BigDecimal getInterBranchDebt() { return interBranchDebt; }
    public void setInterBranchDebt(BigDecimal interBranchDebt) { this.interBranchDebt = interBranchDebt; }
    
    public String getReleasePasscode() { return releasePasscode; }
    public void setReleasePasscode(String releasePasscode) { this.releasePasscode = releasePasscode; }
    
    public BigDecimal getTotalFees() { return totalFees; }
    public void setTotalFees(BigDecimal totalFees) { this.totalFees = totalFees; }
    
    public String getSenderBranchName() { return senderBranchName; }
    public void setSenderBranchName(String senderBranchName) { this.senderBranchName = senderBranchName; }
    
    public String getReceiverBranchName() { return receiverBranchName; }
    public void setReceiverBranchName(String receiverBranchName) { this.receiverBranchName = receiverBranchName; }
}
