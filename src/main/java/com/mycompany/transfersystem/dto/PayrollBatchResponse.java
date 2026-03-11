package com.mycompany.transfersystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayrollBatchResponse {
    private int successCount;
    private int failedCount;
}
