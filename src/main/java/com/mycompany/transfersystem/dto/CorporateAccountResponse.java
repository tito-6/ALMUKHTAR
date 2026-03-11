package com.mycompany.transfersystem.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CorporateAccountResponse {
    private Long id;
    private Long parentUserId;
    private Long subUserId;
    private String accountLabel;
    private BigDecimal spendingLimit;
    private boolean active;
}
