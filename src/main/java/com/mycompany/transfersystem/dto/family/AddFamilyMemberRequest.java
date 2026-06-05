package com.mycompany.transfersystem.dto.family;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddFamilyMemberRequest {
    @NotNull
    private Long memberUserId;
    private BigDecimal memberLimit;
}
