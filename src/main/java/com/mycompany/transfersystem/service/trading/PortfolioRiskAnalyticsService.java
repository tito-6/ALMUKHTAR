package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.entity.Position;
import com.mycompany.transfersystem.entity.TradingAccount;
import com.mycompany.transfersystem.entity.enums.AssetClass;
import com.mycompany.transfersystem.repository.PositionRepository;
import com.mycompany.transfersystem.repository.TradingAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class PortfolioRiskAnalyticsService {

    private final TradingAccountRepository tradingAccountRepository;
    private final PositionRepository positionRepository;

    public PortfolioRiskAnalyticsService(TradingAccountRepository tradingAccountRepository,
                                         PositionRepository positionRepository) {
        this.tradingAccountRepository = tradingAccountRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildRiskSnapshot(Long userId) {
        TradingAccount account = tradingAccountRepository.findByUser_Id(userId)
                .orElseThrow(() -> new com.mycompany.transfersystem.exception.ResourceNotFoundException("Trading account not found"));
        List<Position> positions = positionRepository.findByTradingAccount_Id(account.getId());

        BigDecimal cash = account.getBuyingPowerUsd() != null ? account.getBuyingPowerUsd() : BigDecimal.ZERO;
        BigDecimal positionsValue = BigDecimal.ZERO;
        Map<String, BigDecimal> bySymbol = new LinkedHashMap<>();
        Map<String, BigDecimal> byClass = new LinkedHashMap<>();

        for (Position p : positions) {
            BigDecimal px = p.getCurrentPrice() != null ? p.getCurrentPrice() : p.getAvgEntryPrice();
            BigDecimal mv = p.getQuantity().multiply(px).setScale(4, RoundingMode.HALF_UP);
            positionsValue = positionsValue.add(mv);
            bySymbol.merge(p.getSymbol(), mv, BigDecimal::add);
            AssetClass ac = TradingFeeService.detectAssetClass(p.getSymbol());
            byClass.merge(ac.name(), mv, BigDecimal::add);
        }

        BigDecimal totalValue = cash.add(positionsValue).setScale(4, RoundingMode.HALF_UP);

        Map<String, BigDecimal> concSym = toPct(bySymbol, totalValue);
        Map<String, BigDecimal> concClass = toPct(byClass, totalValue);

        BigDecimal totalPnl = BigDecimal.ZERO;
        BigDecimal dayPnl = BigDecimal.ZERO;
        Instant startDay = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Position biggestWinner = null;
        Position biggestLoser = null;
        for (Position p : positions) {
            BigDecimal u = p.getUnrealizedPnl() != null ? p.getUnrealizedPnl() : BigDecimal.ZERO;
            totalPnl = totalPnl.add(u);
            if (p.getLastUpdated() != null && !p.getLastUpdated().isBefore(startDay)) {
                dayPnl = dayPnl.add(u);
            }
            if (biggestWinner == null || u.compareTo(
                    biggestWinner.getUnrealizedPnl() != null ? biggestWinner.getUnrealizedPnl() : BigDecimal.ZERO) > 0) {
                biggestWinner = p;
            }
            if (biggestLoser == null || u.compareTo(
                    biggestLoser.getUnrealizedPnl() != null ? biggestLoser.getUnrealizedPnl() : BigDecimal.ZERO) < 0) {
                biggestLoser = p;
            }
        }

        List<String> warnings = new ArrayList<>();
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> e : concSym.entrySet()) {
                if (e.getValue().compareTo(new BigDecimal("40")) > 0) {
                    warnings.add("Position " + e.getKey() + " is " + e.getValue().setScale(1, RoundingMode.HALF_UP)
                            + "% of portfolio (>40% concentration risk).");
                }
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalValue", totalValue);
        out.put("cashBalance", cash);
        out.put("concentrationBySymbol", concSym);
        out.put("concentrationByAssetClass", concClass);
        out.put("biggestWinner", biggestWinner != null ? Map.of(
                "symbol", biggestWinner.getSymbol(),
                "unrealizedPnl", Optional.ofNullable(biggestWinner.getUnrealizedPnl()).orElse(BigDecimal.ZERO)
        ) : null);
        out.put("biggestLoser", biggestLoser != null ? Map.of(
                "symbol", biggestLoser.getSymbol(),
                "unrealizedPnl", Optional.ofNullable(biggestLoser.getUnrealizedPnl()).orElse(BigDecimal.ZERO)
        ) : null);
        out.put("dayPnl", dayPnl);
        out.put("totalPnl", totalPnl);
        out.put("riskWarnings", warnings);
        return out;
    }

    private Map<String, BigDecimal> toPct(Map<String, BigDecimal> weights, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of();
        }
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : weights.entrySet()) {
            m.put(e.getKey(), e.getValue().multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP));
        }
        return m;
    }
}
