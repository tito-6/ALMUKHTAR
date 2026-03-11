package com.mycompany.transfersystem.dto.wallet;

import com.mycompany.transfersystem.entity.enums.KycTier;
import com.mycompany.transfersystem.entity.enums.WalletStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class WalletResponse {

    private Long id;
    private Long userId;
    private String walletNumber;
    private WalletStatus status;
    private KycTier kycTier;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private Instant createdAt;
    private List<WalletBalanceDto> balances;

    @Data
    public static class WalletBalanceDto {
        private String currencyCode;
        private BigDecimal availableBalance;
        private BigDecimal lockedBalance;
    }
}
