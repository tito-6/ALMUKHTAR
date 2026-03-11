package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.*;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.accounting.AccountingLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

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

    public OrderExecutionService(TradingAccountRepository tradingAccountRepository,
                                 OrderRepository orderRepository,
                                 PositionRepository positionRepository,
                                 PriceCacheService priceCacheService,
                                 YahooFinanceService yahooFinanceService,
                                 TradingFeeService tradingFeeService,
                                 AccountingLedgerService accountingLedgerService,
                                 AuditService auditService) {
        this.tradingAccountRepository = tradingAccountRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.priceCacheService = priceCacheService;
        this.yahooFinanceService = yahooFinanceService;
        this.tradingFeeService = tradingFeeService;
        this.accountingLedgerService = accountingLedgerService;
        this.auditService = auditService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order placeOrder(Long tradingAccountId, String symbol, OrderSide side, BigDecimal quantity,
                           BigDecimal limitPrice, User actor) {
        TradingAccount account = tradingAccountRepository.findById(tradingAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Trading account not found"));

        BigDecimal price = priceCacheService.get(symbol);
        if (price == null) {
            price = yahooFinanceService.getQuote(symbol).orElseThrow(
                    () -> new IllegalStateException("Could not get quote for " + symbol));
            priceCacheService.set(symbol, price);
        }
        BigDecimal orderValue = quantity.multiply(limitPrice != null ? limitPrice : price);
        AssetClass assetClass = TradingFeeService.detectAssetClass(symbol);
        BigDecimal fee = tradingFeeService.calculateFee(assetClass, orderValue);

        if (side == OrderSide.BUY) {
            BigDecimal totalRequired = orderValue.add(fee);
            if (account.getBuyingPowerUsd().compareTo(totalRequired) < 0) {
                throw new IllegalStateException("Insufficient buying power");
            }
            account.setBuyingPowerUsd(account.getBuyingPowerUsd().subtract(totalRequired));
        } else {
            Position pos = positionRepository.findByTradingAccount_IdAndSymbol(account.getId(), symbol)
                    .orElseThrow(() -> new IllegalStateException("No position to sell for " + symbol));
            if (pos.getQuantity().compareTo(quantity) < 0) {
                throw new IllegalStateException("Insufficient position quantity");
            }
            BigDecimal proceeds = orderValue.subtract(fee);
            account.setBuyingPowerUsd(account.getBuyingPowerUsd().add(proceeds));
        }
        tradingAccountRepository.save(account);

        Order order = Order.builder()
                .tradingAccount(account)
                .symbol(symbol)
                .orderType(limitPrice != null ? OrderType.LIMIT : OrderType.MARKET)
                .side(side)
                .quantity(quantity)
                .limitPrice(limitPrice)
                .status(OrderStatus.PENDING)
                .platformFee(fee)
                .platformFeeCurrency("USD")
                .build();
        order = orderRepository.save(order);

        BigDecimal fillPrice = limitPrice != null ? limitPrice : price;
        order.setFilledPrice(fillPrice);
        order.setFilledAt(Instant.now());
        order.setStatus(OrderStatus.FILLED);
        orderRepository.save(order);

        updatePosition(account, symbol, quantity, fillPrice, side);

        if (fee.compareTo(BigDecimal.ZERO) > 0 && actor != null) {
            accountingLedgerService.postDoubleEntryForReference(
                    "TRADING_FEE", order.getId(),
                    "TRADING_ACCOUNT_" + account.getId(),
                    "PLATFORM_OWNER_REVENUE",
                    fee, "USD", "Trading fee " + symbol, actor);
        }

        auditService.log("ORDER_FILLED", "ORDER", order.getId(),
                String.format("symbol=%s side=%s qty=%s price=%s fee=%s", symbol, side, quantity, fillPrice, fee), actor);
        return order;
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
                        .add(fillPrice.multiply(quantity)).divide(totalQty, 4, java.math.RoundingMode.HALF_UP);
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
        if (pos != null) positionRepository.save(pos);
    }
}
