package com.mycompany.transfersystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PayrollBatchRequest {
    @NotNull
    private List<PayrollItem> items;

    @Data
    public static class PayrollItem {
        @NotNull
        private Long receiverId;
        @NotNull
        private Long fundId;
        @NotNull
        private BigDecimal amount;
    }
}
