package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.entity.PriceAlert;
import com.mycompany.transfersystem.entity.TradableAsset;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.PriceAlertKind;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.PriceAlertRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final UserRepository userRepository;
    private final TradableAssetResolutionService tradableAssetResolutionService;
    private final PriceCacheService priceCacheService;
    private final YahooFinanceService yahooFinanceService;
    private final TradingOutcomeNotificationService tradingOutcomeNotificationService;

    public PriceAlertService(PriceAlertRepository priceAlertRepository,
                             UserRepository userRepository,
                             TradableAssetResolutionService tradableAssetResolutionService,
                             PriceCacheService priceCacheService,
                             YahooFinanceService yahooFinanceService,
                             TradingOutcomeNotificationService tradingOutcomeNotificationService) {
        this.priceAlertRepository = priceAlertRepository;
        this.userRepository = userRepository;
        this.tradableAssetResolutionService = tradableAssetResolutionService;
        this.priceCacheService = priceCacheService;
        this.yahooFinanceService = yahooFinanceService;
        this.tradingOutcomeNotificationService = tradingOutcomeNotificationService;
    }

    @Transactional(readOnly = true)
    public List<PriceAlert> listForUser(Long userId) {
        return priceAlertRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public PriceAlert create(User user, String symbol, PriceAlertKind kind,
                             BigDecimal thresholdPrice, BigDecimal thresholdPercent,
                             boolean notifyInApp, boolean notifyWhatsApp) {
        String sym = symbol.trim().toUpperCase();
        tradableAssetResolutionService.resolveEnabled(sym, user.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Symbol is not available for alerts"));

        BigDecimal ref = priceCacheService.get(sym);
        Long baselineVol = null;
        if (ref == null) {
            YahooFinanceService.MarketQuote mq = yahooFinanceService.getQuoteDetails(sym)
                    .orElseThrow(() -> new IllegalStateException("Could not load quote for alert baseline"));
            ref = mq.price();
            priceCacheService.set(sym, ref);
            baselineVol = mq.volume();
        } else if (kind == PriceAlertKind.VOLUME_SPIKE) {
            YahooFinanceService.MarketQuote mq = yahooFinanceService.getQuoteDetails(sym).orElse(null);
            if (mq != null) {
                baselineVol = mq.volume();
            }
        }

        PriceAlert alert = PriceAlert.builder()
                .userId(user.getId())
                .symbol(sym)
                .kind(kind)
                .thresholdPrice(thresholdPrice)
                .thresholdPercent(thresholdPercent)
                .baselineVolume(baselineVol != null ? baselineVol : 0L)
                .referencePrice(ref)
                .notifyInApp(notifyInApp)
                .notifyWhatsApp(notifyWhatsApp)
                .build();
        return priceAlertRepository.save(alert);
    }

    @Transactional
    public void deleteOwned(Long userId, Long alertId) {
        PriceAlert a = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        if (!a.getUserId().equals(userId)) {
            throw new com.mycompany.transfersystem.exception.ConditionNotMetException("Alert belongs to another user");
        }
        priceAlertRepository.delete(a);
    }

    /**
     * Scans open alerts using cached quotes (and Yahoo fallback), marks triggered, and notifies once.
     */
    @Transactional
    public void evaluateOpenAlerts() {
        List<PriceAlert> open = priceAlertRepository.findByTriggeredIsFalse();
        for (PriceAlert alert : open) {
            try {
                evaluateOne(alert);
            } catch (Exception ignored) {
                // continue other alerts
            }
        }
    }

    private void evaluateOne(PriceAlert alert) {
        String sym = alert.getSymbol();
        BigDecimal price = priceCacheService.get(sym);
        YahooFinanceService.MarketQuote mq = null;
        if (price == null) {
            mq = yahooFinanceService.getQuoteDetails(sym).orElse(null);
            if (mq == null || mq.price() == null) {
                return;
            }
            price = mq.price();
            priceCacheService.set(sym, price);
        }
        if (mq == null) {
            mq = yahooFinanceService.getQuoteDetails(sym).orElse(null);
        }
        Long volume = mq != null ? mq.volume() : null;

        boolean fire = switch (alert.getKind()) {
            case ABOVE_PRICE -> alert.getThresholdPrice() != null && price.compareTo(alert.getThresholdPrice()) >= 0;
            case BELOW_PRICE -> alert.getThresholdPrice() != null && price.compareTo(alert.getThresholdPrice()) <= 0;
            case PERCENT_MOVE_UP -> {
                BigDecimal ref = alert.getReferencePrice();
                if (ref == null || ref.compareTo(BigDecimal.ZERO) == 0 || alert.getThresholdPercent() == null) {
                    yield false;
                }
                BigDecimal pct = price.subtract(ref).divide(ref, 8, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                yield pct.compareTo(alert.getThresholdPercent()) >= 0;
            }
            case PERCENT_MOVE_DOWN -> {
                BigDecimal ref = alert.getReferencePrice();
                if (ref == null || ref.compareTo(BigDecimal.ZERO) == 0 || alert.getThresholdPercent() == null) {
                    yield false;
                }
                BigDecimal pct = price.subtract(ref).divide(ref, 8, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                yield pct.compareTo(alert.getThresholdPercent().negate()) <= 0;
            }
            case VOLUME_SPIKE -> {
                if (volume == null || alert.getBaselineVolume() == null || alert.getThresholdPercent() == null) {
                    yield false;
                }
                BigDecimal mult = BigDecimal.ONE.add(
                        alert.getThresholdPercent().divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
                BigDecimal thresholdVol = new BigDecimal(alert.getBaselineVolume()).multiply(mult);
                yield new BigDecimal(volume).compareTo(thresholdVol) >= 0;
            }
        };

        if (!fire) {
            return;
        }

        alert.setTriggered(true);
        alert.setTriggeredAt(Instant.now());
        priceAlertRepository.save(alert);

        User user = userRepository.findById(alert.getUserId()).orElse(null);
        if (user == null) {
            return;
        }
        String detail = "Condition met at price " + price + " for " + sym + " (" + alert.getKind() + ")";
        tradingOutcomeNotificationService.notifyFromPriceAlert(user, alert, detail);

        if (alert.isNotifyWhatsApp()) {
            alert.setWhatsappSent(true);
            priceAlertRepository.save(alert);
        }
    }
}
