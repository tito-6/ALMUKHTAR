package com.mycompany.transfersystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LedgerSummaryResponse {
    private Long parentUserId;
    private long subAccountCount;
}
