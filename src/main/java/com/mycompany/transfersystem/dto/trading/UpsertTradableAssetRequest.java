package com.mycompany.transfersystem.dto.trading;

import com.mycompany.transfersystem.entity.enums.AssetClass;
import com.mycompany.transfersystem.entity.enums.TradableAssetRiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpsertTradableAssetRequest {
    private Long tenantId;
    private String countryCode;
    private String allowedRolesCsv;
    @NotBlank
    private String symbol;
    @NotNull
    private AssetClass assetClass;
    @NotBlank
    private String displayName;
    private String currency = "USD";
    private String market = "US";
    private boolean enabled = true;
    private BigDecimal minOrderValue;
    private BigDecimal maxOrderValue;
    private TradableAssetRiskLevel riskLevel = TradableAssetRiskLevel.MEDIUM;
}
