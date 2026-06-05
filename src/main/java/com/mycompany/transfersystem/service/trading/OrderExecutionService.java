package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.config.NotificationThresholdProperties;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.entity.Order;
import com.mycompany.transfersystem.entity.Position;
import com.mycompany.transfersystem.entity.TradableAsset;
import com.mycompany.transfersystem.entity.TradingAccount;
import com.mycompany.transfersystem.entity.TradingRiskProfile;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.*;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.exception.TradingOrderFailedException;
import com.mycompany.transfersystem.repository.OrderRepository;
import com.mycompany.transfersystem.repository.PositionRepository;
import com.mycompany.transfersystem.repository.TradingAccountRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.accounting.AccountingLedgerService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Service
public class OrderExecutionService {

    private final TradingAccountRepository tradingAccountRepository;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final PriceCacheService priceCacheService;
    private final YahooFinanceService yahooFinanceService;
    private final TradingFeeService tradingFeeService;
    private final AccountingLedgerService accountingLedgerService;
    private final AuditService auditService;
    private final TradableAssetResolutionService tradableAssetResolutionService;
    private final TradingRiskProfileService tradingRiskProfileService;
    private final TradingOutcomeNotificationService tradingOutcomeNotificationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationThresholdProperties notificationThresholdProperties;

    public OrderExecutionService(TradingAccountRepository tradingAccountRepository,
                                 OrderRepository orderRepository,
                                 PositionRepository positionRepository,
                                 PriceCacheService priceCacheService,
                                 YahooFinanceService yahooFinanceService,
                                 TradingFeeService tradingFeeService,
                                 AccountingLedgerService accountingLedgerService,
                                 AuditService auditService,
                                 TradableAssetResolutionService tradableAssetResolutionService,
                                 TradingRiskProfileService tradingRiskProfileService,
                                 TradingOutcomeNotificationService tradingOutcomeNotificationService,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 NotificationThresholdProperties notificationThresholdProperties) {
        this.tradingAccountRepository = tradingAccountRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.priceCacheService = priceCacheService;
        this.yahooFinanceService = yahooFinanceService;
        this.tradingFeeService = tradingFeeService;
        this.accountingLedgerService = accountingLedgerService;
        this.auditService = auditService;
        this.tradableAssetResolutionService = tradableAssetResolutionService;
        this.tradingRiskProfileService = tradingRiskProfileService;
        this.tradingOutcomeNotificationService = tradingOutcomeNotificationService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.notificationThresholdProperties = notificationThresholdProperties;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order placeOrder(Long tradingAccountId, String symbol, OrderSide side, BigDecimal quantity,
                            BigDecimal limitPrice, User actor) {
        TradingAccount account = tradingAccountRepository.findById(tradingAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Trading account not found"));
        User owner = account.getUser();
        String sym = symbol == null ? "" : symbol.trim().toUpperCase();

        TradableAsset asset = tradableAssetResolutionService
                .resolveEnabled(sym, owner.getRole())
                .orElse(null);
        if (asset == null || !asset.isEnabled()) {
            failOrder(account, sym, side, quantity, limitPrice, null, null, BigDecimal.ZERO,
                    OrderFailureReason.DISABLED_ASSET,
                    "Symbol is not enabled for trading for your tenant, country, or role.",
                    actor);
        }

        TradingRiskProfile risk = tradingRiskProfileService.getOrCreateDefault(owner);
        tradingRiskProfileService.assertKycAllowsProfile(owner, risk);

        BigDecimal refPrice = priceCacheService.get(sym);
        YahooFinanceService.MarketQuote quoteDetails = null;
        if (refPrice == null) {
            quoteDetails = yahooFinanceService.getQuoteDetails(sym).orElse(null);
            if (quoteDetails == null || quoteDetails.price() == null) {
                failOrder(account, sym, side, quantity, limitPrice, null, null, BigDecimal.ZERO,
                        OrderFailureReason.PROVIDER_FAILURE,
                        "Live market price is unavailable for this symbol.",
                        actor);
            }
            refPrice = quoteDetails.price();
            priceCacheService.set(sym, refPrice);
        }

        BigDecimal fillPrice;
        if (side == OrderSide.BUY) {
            if (limitPrice != null && refPrice.compareTo(limitPrice) > 0) {
                failOrder(account, sym, side, quantity, limitPrice, refPrice, null, BigDecimal.ZERO,
                        OrderFailureReason.PRICE_MOVEMENT,
                        "Limit buy is below the current market; the order would not fill at your limit.",
                        actor);
            }
            fillPrice = limitPrice == null ? refPrice : refPrice.min(limitPrice);
        } else {
            if (limitPrice != null && refPrice.compareTo(limitPrice) < 0) {
                failOrder(account, sym, side, quantity, limitPrice, refPrice, null, BigDecimal.ZERO,
                        OrderFailureReason.PRICE_MOVEMENT,
                        "Limit sell is above the current market; the order would not fill at your limit.",
                        actor);
            }
            fillPrice = limitPrice == null ? refPrice : refPrice.max(limitPrice);
        }

        BigDecimal orderValue = quantity.multiply(fillPrice).setScale(4, RoundingMode.HALF_UP);
        AssetClass assetClass = asset.getAssetClass();
        tradingRiskProfileService.assertAssetClassAllowed(risk, assetClass);

        if (asset.getMinOrderValue() != null && orderValue.compareTo(asset.getMinOrderValue()) < 0) {
            failOrder(account, sym, side, quantity, limitPrice, refPrice, fillPrice, BigDecimal.ZERO,
                    OrderFailureReason.KYC_RISK_LIMIT,
                    "Order value is below the minimum permitted for this instrument.",
                    actor);
        }
        if (asset.getMaxOrderValue() != null && orderValue.compareTo(asset.getMaxOrderValue()) > 0) {
            failOrder(account, sym, side, quantity, limitPrice, refPrice, fillPrice, BigDecimal.ZERO,
                    OrderFailureReason.KYC_RISK_LIMIT,
                    "Order value exceeds the maximum permitted for this instrument.",
                    actor);
        }

        if (orderValue.compareTo(risk.getMaxSingleOrderUsd()) > 0) {
            failOrder(account, sym, side, quantity, limitPrice, refPrice, fillPrice, BigDecimal.ZERO,
                    OrderFailureReason.KYC_RISK_LIMIT,
                    "Order exceeds your maximum single-order amount for your risk profile.",
                    actor);
        }

        Instant startUtcDay = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal usedToday = orderRepository.sumFilledNotionalUsdSince(account.getId(), startUtcDay);
        if (usedToday == null) {
            usedToday = BigDecimal.ZERO;
        }
        if (usedToday.add(orderValue).compareTo(risk.getDailyTradingCapUsd()) > 0) {
            failOrder(account, sym, side, quantity, limitPrice, refPrice, fillPrice, BigDecimal.ZERO,
                    OrderFailureReason.KYC_RISK_LIMIT,
                    "This order would exceed your daily trading cap.",
                    actor);
        }

        BigDecimal fee = tradingFeeService.calculateFee(assetClass, orderValue);
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            fee = BigDecimal.ZERO;
        }

        if (side == OrderSide.BUY) {
            BigDecimal totalRequired = orderValue.add(fee);
            if (account.getBuyingPowerUsd().compareTo(totalRequired) < 0) {
                failOrder(account, sym, side, quantity, limitPrice, refPrice, fillPrice, fee,
                        OrderFailureReason.FUNDS,
                        "Insufficient buying power for this order including the reserved platform fee.",
                        actor);
            }
            account.setBuyingPowerUsd(account.getBuyingPowerUsd().subtract(totalRequired));
        } else {
            Position pos = positionRepository.findByTradingAccount_IdAndSymbol(account.getId(), sym)
                    .orElse(null);
            if (pos == null || pos.getQuantity().compareTo(quantity) < 0) {
                failOrder(account, sym, side, quantity, limitPrice, refPrice, fillPrice, fee,
                        OrderFailureReason.INSUFFICIENT_POSITION,
                        "You do not hold enough of this symbol to sell the requested quantity.",
                        actor);
            }
            BigDecimal proceeds = orderValue.subtract(fee);
            account.setBuyingPowerUsd(account.getBuyingPowerUsd().add(proceeds));
        }
        tradingAccountRepository.save(account);

        Order order = Order.builder()
                .tradingAccount(account)
                .symbol(sym)
                .orderType(limitPrice != null ? OrderType.LIMIT : OrderType.MARKET)
                .side(side)
                .quantity(quantity)
                .limitPrice(limitPrice)
                .status(OrderStatus.PENDING)
                .platformFee(fee)
                .platformFeeCurrency("USD")
                .referenceMarketPrice(refPrice)
                .build();
        order = orderRepository.save(order);

        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TradingOrderPlacedEvent(
                order.getId(), account.getId(), owner.getId(), sym, side.name(), quantity, fee));

        try {
            order.setFilledPrice(fillPrice);
            order.setFilledAt(Instant.now());
            order.setStatus(OrderStatus.FILLED);
            orderRepository.save(order);

            boolean highTrade = orderValue.compareTo(notificationThresholdProperties.getMerchantPerTradePlatformNotify()) >= 0;
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TradingOrderFilledEvent(
                    order.getId(), account.getId(), owner.getId(), sym, side.name(), quantity, fillPrice, fee,
                    highTrade));

            updatePosition(account, sym, quantity, fillPrice, side);

            if (fee.compareTo(BigDecimal.ZERO) > 0 && actor != null) {
                accountingLedgerService.postDoubleEntryForReference(
                        "TRADING_FEE", order.getId(),
                        "TRADING_ACCOUNT_" + account.getId(),
                        "PLATFORM_OWNER_REVENUE",
                        fee, "USD", "Trading fee " + sym, actor);
            }

            auditService.log("ORDER_FILLED", "ORDER", order.getId(),
                    String.format("symbol=%s side=%s qty=%s price=%s fee=%s reserved=1", sym, side, quantity, fillPrice, fee),
                    actor);
            tradingOutcomeNotificationService.notifyOrderOutcome(owner, order);
            return order;
        } catch (Exception ex) {
            rollbackBuyingPower(account, side, orderValue, fee);
            tradingAccountRepository.save(account);
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason(OrderFailureReason.INTERNAL);
            order.setFailureDetail("Unexpected error while finalizing the trade: " + ex.getMessage());
            orderRepository.save(order);
            auditService.log("ORDER_FAILED", "ORDER", order.getId(), order.getFailureDetail(), actor);
            tradingOutcomeNotificationService.notifyOrderOutcome(owner, order);
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TradingOrderFailedEvent(
                    account.getId(), owner.getId(), sym, "INTERNAL"));
            throw new TradingOrderFailedException(order);
        }
    }

    private void rollbackBuyingPower(TradingAccount account, OrderSide side, BigDecimal orderValue, BigDecimal fee) {
        if (side == OrderSide.BUY) {
            account.setBuyingPowerUsd(account.getBuyingPowerUsd().add(orderValue).add(fee));
        } else {
            BigDecimal proceeds = orderValue.subtract(fee);
            account.setBuyingPowerUsd(account.getBuyingPowerUsd().subtract(proceeds));
        }
    }

    private void failOrder(TradingAccount account, String symbol, OrderSide side,
                                                  BigDecimal quantity, BigDecimal limitPrice,
                                                  BigDecimal refPrice, BigDecimal fillPrice, BigDecimal fee,
                                                  OrderFailureReason reason, String detail, User actor) {
        Order order = Order.builder()
                .tradingAccount(account)
                .symbol(symbol)
                .orderType(limitPrice != null ? OrderType.LIMIT : OrderType.MARKET)
                .side(side)
                .quantity(quantity)
                .limitPrice(limitPrice)
                .status(OrderStatus.FAILED)
                .failureReason(reason)
                .failureDetail(detail)
                .referenceMarketPrice(refPrice)
                .filledPrice(fillPrice)
                .platformFee(fee != null ? fee : BigDecimal.ZERO)
                .platformFeeCurrency("USD")
                .build();
        order = orderRepository.save(order);
        auditService.log("ORDER_FAILED", "ORDER", order.getId(),
                "reason=" + reason + " " + detail, actor);
        tradingOutcomeNotificationService.notifyOrderOutcome(account.getUser(), order);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TradingOrderFailedEvent(
                account.getId(), account.getUser().getId(), symbol, reason.name()));
        throw new TradingOrderFailedException(order);
    }

    private void updatePosition(TradingAccount account, String symbol, BigDecimal quantity, BigDecimal fillPrice, OrderSide side) {
        Position pos = positionRepository.findByTradingAccount_IdAndSymbol(account.getId(), symbol).orElse(null);
        if (side == OrderSide.BUY) {
            if (pos == null) {
                pos = Position.builder()
                        .tradingAccount(account)
                        .symbol(symbol)
                        .quantity(quantity)
                        .avgEntryPrice(fillPrice)
                        .currentPrice(fillPrice)
                        .unrealizedPnl(BigDecimal.ZERO)
                        .build();
            } else {
                BigDecimal totalQty = pos.getQuantity().add(quantity);
                BigDecimal avgPrice = pos.getAvgEntryPrice().multiply(pos.getQuantity())
                        .add(fillPrice.multiply(quantity)).divide(totalQty, 4, RoundingMode.HALF_UP);
                pos.setQuantity(totalQty);
                pos.setAvgEntryPrice(avgPrice);
                pos.setCurrentPrice(fillPrice);
            }
        } else {
            if (pos != null) {
                pos.setQuantity(pos.getQuantity().subtract(quantity));
                if (pos.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    positionRepository.delete(pos);
                    return;
                }
                pos.setCurrentPrice(fillPrice);
            }
        }
        if (pos != null) {
            positionRepository.save(pos);
        }
    }
}
