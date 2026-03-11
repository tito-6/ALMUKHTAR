package com.mycompany.transfersystem.dto.lending;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RepaymentScheduleResponse {

    private Long loanId;
    private List<RepaymentScheduleItemDto> instalments;
}
