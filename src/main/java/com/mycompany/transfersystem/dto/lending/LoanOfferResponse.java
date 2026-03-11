package com.mycompany.transfersystem.dto.lending;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LoanOfferResponse {

    private Long applicationId;
    private BigDecimal approvedAmount;
    private BigDecimal monthlyPayment;
    private BigDecimal totalInterest;
    private BigDecimal apr;
    private int termDays;
    private List<RepaymentScheduleItemDto> schedulePreview;
}
