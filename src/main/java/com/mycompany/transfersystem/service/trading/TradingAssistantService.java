package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.dto.trading.TradingAssistantResponse;
import com.mycompany.transfersystem.entity.Position;
import com.mycompany.transfersystem.entity.TradableAsset;
import com.mycompany.transfersystem.entity.TradingRiskProfile;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.OrderSide;
import com.mycompany.transfersystem.entity.enums.PriceAlertKind;
import com.mycompany.transfersystem.repository.PositionRepository;
import com.mycompany.transfersystem.repository.TradableAssetRepository;
import com.mycompany.transfersystem.repository.TradingAccountRepository;
import com.mycompany.transfersystem.service.ai.AlmukhtarAiService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-safe trading helper: explains concepts, never promises profit, never recommends disabled symbols.
 * Optional LLM enrichment when {@code almukhtar.ai.enabled=true}.
 */
@Service
public class TradingAssistantService {

    private static final String DISCLAIMER = "Educational guidance only. Markets involve risk of loss. "
            + "ALMUKHTAR does not guarantee returns. Draft alerts and orders require your explicit confirmation in the app.";

    private final TradingRiskProfileService tradingRiskProfileService;
    private final TradableAssetRepository tradableAssetRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final PositionRepository positionRepository;
    private final ObjectProvider<AlmukhtarAiService> aiService;

    public TradingAssistantService(TradingRiskProfileService tradingRiskProfileService,
                                   TradableAssetRepository tradableAssetRepository,
                                   TradingAccountRepository tradingAccountRepository,
                                   PositionRepository positionRepository,
                                   ObjectProvider<AlmukhtarAiService> aiService) {
        this.tradingRiskProfileService = tradingRiskProfileService;
        this.tradableAssetRepository = tradableAssetRepository;
        this.tradingAccountRepository = tradingAccountRepository;
        this.positionRepository = positionRepository;
        this.aiService = aiService;
    }

    public TradingAssistantResponse assist(User user, String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        TradingRiskProfile risk = tradingRiskProfileService.getOrCreateDefault(user);
        Set<String> enabledSymbols = tradableAssetRepository.findAll().stream()
                .filter(TradableAsset::isEnabled)
                .map(a -> a.getSymbol().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Object> draftAlert = maybeDraftAlert(lower, enabledSymbols);
        Map<String, Object> draftOrder = maybeDraftOrder(lower, enabledSymbols);

        String heuristic = buildHeuristicAnswer(user, lower, risk, enabledSymbols);

        String answer = heuristic;
        AlmukhtarAiService ai = aiService.getIfAvailable();
        if (ai != null) {
            String guardedPrompt = """
                    You are a conservative trading educator for Syrian users and corporate treasurers using ALMUKHTAR.
                    HARD RULES:
                    - Never promise profit or future returns.
                    - Never recommend buying/selling specific tickers unless they appear in this allow-list: %s
                    - If asked for unavailable symbols, explain they are blocked until enabled by the tenant.
                    - Explain order types, P&L concepts, volatility, concentration, and risk plainly.
                    - Remind that draft alerts/orders must be confirmed in UI.
                    Context:
                    %s
                    User message:
                    %s
                    """.formatted(
                    String.join(", ", enabledSymbols.isEmpty() ? List.of("(none configured)") : enabledSymbols),
                    heuristic,
                    message);
            answer = ai.chat(guardedPrompt);
        }

        return TradingAssistantResponse.builder()
                .answer(answer)
                .draftPriceAlert(draftAlert)
                .draftOrder(draftOrder)
                .requiresConfirmation(true)
                .disclaimer(DISCLAIMER)
                .build();
    }

    private Map<String, Object> maybeDraftAlert(String lower, Set<String> enabled) {
        if (!lower.contains("alert")) {
            return null;
        }
        String sym = enabled.stream().filter(lower::contains).findFirst().orElse(null);
        if (sym == null) {
            return Map.of("note", "No enabled symbol detected in your message; open Price Alerts after you pick a listed symbol.");
        }
        return Map.of(
                "symbol", sym,
                "kind", PriceAlertKind.ABOVE_PRICE.name(),
                "thresholdPrice", "SET_IN_UI",
                "notifyInApp", true,
                "notifyWhatsApp", false
        );
    }

    private Map<String, Object> maybeDraftOrder(String lower, Set<String> enabled) {
        if (!lower.contains("order") && !lower.contains("buy") && !lower.contains("sell")) {
            return null;
        }
        String sym = enabled.stream().filter(lower::contains).findFirst().orElse(null);
        OrderSide side = lower.contains("sell") ? OrderSide.SELL : OrderSide.BUY;
        if (sym == null) {
            return Map.of("note", "No enabled symbol detected; place orders only on tenant-approved symbols.");
        }
        return Map.of(
                "symbol", sym,
                "side", side.name(),
                "quantity", "SET_IN_UI",
                "limitPrice", lower.contains("limit") ? "OPTIONAL" : null
        );
    }

    private String buildHeuristicAnswer(User user, String lower, TradingRiskProfile risk, Set<String> enabled) {
        var accountOpt = tradingAccountRepository.findByUser_Id(user.getId());
        if (accountOpt.isEmpty()) {
            return "Open a trading account first. Your risk profile is " + risk.getArchetype()
                    + " with a daily cap of " + risk.getDailyTradingCapUsd() + " USD notional on fills.";
        }
        var positions = positionRepository.findByTradingAccount_Id(accountOpt.get().getId());
        if (lower.contains("concentration") || lower.contains("concentr")) {
            return concentrationSummary(positions, accountOpt.get().getBuyingPowerUsd());
        }
        if (lower.contains("p&l") || lower.contains("pnl") || lower.contains("profit")) {
            return "P&L measures gains or losses versus your entry price; it is not a prediction of future performance. "
                    + "Past P&L does not guarantee future results.";
        }
        if (lower.contains("volatility")) {
            return "Volatility describes how wide prices swing. Higher volatility usually means higher risk and wider stop distances.";
        }
        if (lower.contains("order type") || lower.contains("limit") || lower.contains("market")) {
            return "Market orders aim for immediate execution at the current quote. Limit orders only execute at your price or better; "
                    + "they may not fill if the market never reaches your limit.";
        }
        if (lower.contains("risk")) {
            return "Your profile is " + risk.getArchetype() + " with caps " + risk.getMaxSingleOrderUsd()
                    + " USD per order and " + risk.getDailyTradingCapUsd() + " USD per day on filled notional. "
                    + "Enabled symbols: " + (enabled.isEmpty() ? "(admin has not published a list yet)" : String.join(", ", enabled));
        }
        return "I can clarify P&L, order types, volatility, portfolio concentration, and risk limits. "
                + "I cannot promise profits or suggest securities outside the enabled list.";
    }

    private String concentrationSummary(List<Position> positions, BigDecimal cash) {
        BigDecimal posVal = BigDecimal.ZERO;
        for (Position p : positions) {
            BigDecimal px = p.getCurrentPrice() != null ? p.getCurrentPrice() : p.getAvgEntryPrice();
            posVal = posVal.add(p.getQuantity().multiply(px));
        }
        BigDecimal total = posVal.add(cash != null ? cash : BigDecimal.ZERO);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return "No portfolio value yet.";
        }
        StringBuilder sb = new StringBuilder();
        for (Position p : positions) {
            BigDecimal px = p.getCurrentPrice() != null ? p.getCurrentPrice() : p.getAvgEntryPrice();
            BigDecimal mv = p.getQuantity().multiply(px);
            BigDecimal pct = mv.multiply(new BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP);
            sb.append(p.getSymbol()).append(" ~").append(pct).append("%; ");
        }
        return "Estimated concentration by market value: " + sb
                + "If one line is above 40% of the portfolio, diversification risk is elevated.";
    }
}
