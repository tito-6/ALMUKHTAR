package com.mycompany.transfersystem.dto.lending;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class LoanResponse {

    private Long id;
    private BigDecimal principalAmount;
    private String currency;
    private Instant disbursedAt;
    private int termDays;
    private BigDecimal monthlyPayment;
    private BigDecimal outstandingBalance;
    private String status;
}
