package com.mycompany.transfersystem.dto.liquidity;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ReconciliationReport {
    Long branchId;
    boolean mismatchDetected;
    String detail;
    LocalDateTime reconciledAt;
}
