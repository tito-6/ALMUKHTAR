package com.mycompany.transfersystem.dto.batch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchProgressResponse {

    private Long jobId;
    private int processedRows;
    private int successCount;
    private int failedCount;
    private double percentComplete;
    private String estimatedRemaining;
}
