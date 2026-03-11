package com.mycompany.transfersystem.dto.batch;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BatchUploadResponse {

    private Long jobId;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private BigDecimal estimatedTotalUsd;
    private List<String> validationErrors;
}
