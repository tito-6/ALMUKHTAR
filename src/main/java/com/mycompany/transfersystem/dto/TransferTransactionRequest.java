package com.mycompany.transfersystem.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferTransactionRequest {
    
    @NotNull(message = "Sender ID is required")
    private Long senderId;
    
    @NotNull(message = "Receiver ID is required")
    private Long receiverId;
    
    @NotNull(message = "Fund ID is required")
    private Long fundId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Source currency is required")
    private String sourceCurrency;
    
    @NotBlank(message = "Destination currency is required")
    private String destinationCurrency;
    
    @NotNull(message = "Sender branch ID is required")
    private Long senderBranchId;
    
    @NotNull(message = "Receiver branch ID is required")
    private Long receiverBranchId;

    // Manual getters and setters for Lombok compatibility
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    
    public Long getFundId() { return fundId; }
    public void setFundId(Long fundId) { this.fundId = fundId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getSourceCurrency() { return sourceCurrency; }
    public void setSourceCurrency(String sourceCurrency) { this.sourceCurrency = sourceCurrency; }
    
    public String getDestinationCurrency() { return destinationCurrency; }
    public void setDestinationCurrency(String destinationCurrency) { this.destinationCurrency = destinationCurrency; }
    
    public Long getSenderBranchId() { return senderBranchId; }
    public void setSenderBranchId(Long senderBranchId) { this.senderBranchId = senderBranchId; }
    
    public Long getReceiverBranchId() { return receiverBranchId; }
    public void setReceiverBranchId(Long receiverBranchId) { this.receiverBranchId = receiverBranchId; }
}
