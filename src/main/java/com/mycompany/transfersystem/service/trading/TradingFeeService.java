package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.entity.PlatformTradingFee;
import com.mycompany.transfersystem.entity.enums.AssetClass;
import com.mycompany.transfersystem.repository.PlatformTradingFeeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

@Service
public class TradingFeeService {

    private final PlatformTradingFeeRepository feeRepository;

    public TradingFeeService(PlatformTradingFeeRepository feeRepository) {
        this.feeRepository = feeRepository;
    }

    public BigDecimal calculateFee(AssetClass assetClass, BigDecimal orderValueUsd) {
        Optional<PlatformTradingFee> opt = feeRepository.findFirstByAssetClassAndEffectiveFromBeforeOrderByEffectiveFromDesc(
                assetClass, Instant.now());
        if (opt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        PlatformTradingFee fee = opt.get();
        BigDecimal feeAmount = orderValueUsd.multiply(BigDecimal.valueOf(fee.getFeeRateBps()))
                .divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);
        if (fee.getMinFeeUsd() != null && feeAmount.compareTo(fee.getMinFeeUsd()) < 0) {
            feeAmount = fee.getMinFeeUsd();
        }
        if (fee.getMaxFeeUsd() != null && feeAmount.compareTo(fee.getMaxFeeUsd()) > 0) {
            feeAmount = fee.getMaxFeeUsd();
        }
        return feeAmount;
    }

    public static AssetClass detectAssetClass(String symbol) {
        if (symbol == null) return AssetClass.STOCK;
        String s = symbol.toUpperCase();
        if (s.endsWith("=X") || s.contains("FOREX")) return AssetClass.FOREX;
        if (s.endsWith("-USD") || s.contains("BTC") || s.contains("ETH")) return AssetClass.CRYPTO;
        if (s.equals("SPY") || s.equals("QQQ") || s.length() <= 5 && !s.contains("=")) return AssetClass.ETF;
        return AssetClass.STOCK;
    }
}
