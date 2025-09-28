package com.mycompany.transfersystem.dto;

import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSearchRequest {
    
    // Date range filtering
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // Branch filtering
    private Long senderBranchId;
    private Long receiverBranchId;
    
    // Currency filtering
    private String sourceCurrency;
    private String destinationCurrency;
    
    // Transaction status filtering
    private TransactionStatus status;
    
    // Amount filtering
    private Double minAmount;
    private Double maxAmount;
    
    // User filtering
    private Long senderId;
    private Long receiverId;
    
    // Pagination
    private Integer page = 0;
    private Integer size = 20;
    
    // Sorting
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";

    // Manual getters and setters for Lombok compatibility
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public Long getSenderBranchId() { return senderBranchId; }
    public void setSenderBranchId(Long senderBranchId) { this.senderBranchId = senderBranchId; }
    
    public Long getReceiverBranchId() { return receiverBranchId; }
    public void setReceiverBranchId(Long receiverBranchId) { this.receiverBranchId = receiverBranchId; }
    
    public String getSourceCurrency() { return sourceCurrency; }
    public void setSourceCurrency(String sourceCurrency) { this.sourceCurrency = sourceCurrency; }
    
    public String getDestinationCurrency() { return destinationCurrency; }
    public void setDestinationCurrency(String destinationCurrency) { this.destinationCurrency = destinationCurrency; }
    
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    
    public Double getMinAmount() { return minAmount; }
    public void setMinAmount(Double minAmount) { this.minAmount = minAmount; }
    
    public Double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(Double maxAmount) { this.maxAmount = maxAmount; }
    
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
}
