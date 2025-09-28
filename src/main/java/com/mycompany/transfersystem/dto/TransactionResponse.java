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
public class TransactionResponse {
    private Long id;
    private String senderUsername;
    private String receiverUsername;
    private String fundName;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime createdAt;


}