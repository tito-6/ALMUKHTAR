package com.mycompany.transfersystem.dto.trading;

import com.mycompany.transfersystem.entity.enums.TradingRiskArchetype;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTradingRiskProfileRequest {
    @NotNull
    private TradingRiskArchetype archetype;
}
