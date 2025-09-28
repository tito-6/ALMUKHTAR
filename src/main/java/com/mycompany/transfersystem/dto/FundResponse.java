package com.mycompany.transfersystem.dto;

import com.mycompany.transfersystem.entity.enums.FundStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundResponse {
    private Long id;
    private String name;
    private BigDecimal balance;
    private FundStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}