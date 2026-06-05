package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.*;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.exception.TradingOrderFailedException;
import com.mycompany.transfersystem.repository.OrderRepository;
import com.mycompany.transfersystem.repository.PositionRepository;
import com.mycompany.transfersystem.repository.TradingAccountRepository;
import com.mycompany.transfersystem.service.accounting.AccountingLedgerService;
import com.mycompany.transfersystem.service.trading.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExecutionServiceTest {

    @Mock private TradingAccountRepository tradingAccountRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private PriceCacheService priceCacheService;
    @Mock private YahooFinanceService yahooFinanceService;
    @Mock private TradingFeeService tradingFeeService;
    @Mock private AccountingLedgerService accountingLedgerService;
    @Mock private AuditService auditService;
    @Mock private TradableAssetResolutionService tradableAssetResolutionService;
    @Mock private TradingRiskProfileService tradingRiskProfileService;
    @Mock private TradingOutcomeNotificationService tradingOutcomeNotificationService;
    @Mock private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    @Mock private com.mycompany.transfersystem.config.NotificationThresholdProperties notificationThresholdProperties;

    @InjectMocks private OrderExecutionService orderExecutionService;

    private TradableAsset enabledStock() {
        return TradableAsset.builder()
                .symbol("AAPL")
                .enabled(true)
                .assetClass(AssetClass.STOCK)
                .minOrderValue(BigDecimal.ONE)
                .maxOrderValue(new BigDecimal("999999999"))
                .build();
    }

    private TradingRiskProfile openProfile() {
        return TradingRiskProfile.builder()
                .archetype(TradingRiskArchetype.PROFESSIONAL)
                .maxSingleOrderUsd(new BigDecimal("10000000"))
                .dailyTradingCapUsd(new BigDecimal("100000000"))
                .allowedAssetClasses(EnumSet.allOf(AssetClass.class))
                .build();
    }

    @Test
    void placeOrder_marketBuy_fills_andPostsFeeOnce() {
        User actor = new User();
        actor.setId(1L);
        actor.setRole(UserRole.INDIVIDUAL_USER);
        TradingAccount account = TradingAccount.builder().id(1L).buyingPowerUsd(new BigDecimal("10000")).user(actor).build();
        when(tradingAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(tradableAssetResolutionService.resolveEnabled(eq("AAPL"), any())).thenReturn(Optional.of(enabledStock()));
        when(tradingRiskProfileService.getOrCreateDefault(actor)).thenReturn(openProfile());
        doNothing().when(tradingRiskProfileService).assertKycAllowsProfile(any(), any());
        doNothing().when(tradingRiskProfileService).assertAssetClassAllowed(any(), any());
        when(orderRepository.sumFilledNotionalUsdSince(anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(priceCacheService.get("AAPL")).thenReturn(new BigDecimal("150"));
        when(tradingFeeService.calculateFee(any(), any())).thenReturn(new BigDecimal("1.50"));
        when(orderRepository.save(any())).thenAnswer(i -> {
            Order o = (Order) i.getArgument(0);
            if (o.getId() == null) {
                o.setId(1L);
            }
            return o;
        });
        when(tradingAccountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(positionRepository.findByTradingAccount_IdAndSymbol(anyLong(), anyString())).thenReturn(Optional.empty());
        when(positionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(notificationThresholdProperties.getMerchantPerTradePlatformNotify())
                .thenReturn(new BigDecimal("999999999"));

        Order result = orderExecutionService.placeOrder(1L, "AAPL", OrderSide.BUY, BigDecimal.TEN, null, actor);
        assertNotNull(result);
        assertEquals(OrderStatus.FILLED, result.getStatus());
        verify(accountingLedgerService, times(1)).postDoubleEntryForReference(
                eq("TRADING_FEE"), anyLong(), anyString(), eq("PLATFORM_OWNER_REVENUE"), any(), eq("USD"), anyString(), eq(actor));
        verify(tradingOutcomeNotificationService, times(1)).notifyOrderOutcome(eq(actor), any(Order.class));
    }

    @Test
    void placeOrder_insufficientBalance_doesNotPostFee() {
        User actor = new User();
        actor.setId(1L);
        actor.setRole(UserRole.INDIVIDUAL_USER);
        TradingAccount account = TradingAccount.builder().id(1L).buyingPowerUsd(new BigDecimal("10")).user(actor).build();
        when(tradingAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(tradableAssetResolutionService.resolveEnabled(eq("AAPL"), any())).thenReturn(Optional.of(enabledStock()));
        when(tradingRiskProfileService.getOrCreateDefault(actor)).thenReturn(openProfile());
        doNothing().when(tradingRiskProfileService).assertKycAllowsProfile(any(), any());
        doNothing().when(tradingRiskProfileService).assertAssetClassAllowed(any(), any());
        when(orderRepository.sumFilledNotionalUsdSince(anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(priceCacheService.get("AAPL")).thenReturn(new BigDecimal("150"));
        when(tradingFeeService.calculateFee(any(), any())).thenReturn(new BigDecimal("1.50"));
        when(orderRepository.save(any())).thenAnswer(i -> {
            Order o = (Order) i.getArgument(0);
            o.setId(99L);
            return o;
        });

        assertThrows(TradingOrderFailedException.class, () ->
                orderExecutionService.placeOrder(1L, "AAPL", OrderSide.BUY, BigDecimal.TEN, null, actor));
        verify(accountingLedgerService, never()).postDoubleEntryForReference(any(), any(), any(), any(), any(), any(), any(), any());
        verify(tradingOutcomeNotificationService, atLeastOnce()).notifyOrderOutcome(eq(actor), any(Order.class));
    }

    @Test
    void placeOrder_disabledAsset_rejected() {
        User actor = new User();
        actor.setId(1L);
        actor.setRole(UserRole.INDIVIDUAL_USER);
        TradingAccount account = TradingAccount.builder().id(1L).buyingPowerUsd(new BigDecimal("100000")).user(actor).build();
        when(tradingAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(tradableAssetResolutionService.resolveEnabled(eq("ZZZZ"), any())).thenReturn(Optional.empty());
        when(orderRepository.save(any())).thenAnswer(i -> {
            Order o = (Order) i.getArgument(0);
            o.setId(2L);
            return o;
        });

        assertThrows(TradingOrderFailedException.class, () ->
                orderExecutionService.placeOrder(1L, "ZZZZ", OrderSide.BUY, BigDecimal.ONE, null, actor));
        verify(accountingLedgerService, never()).postDoubleEntryForReference(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void placeOrder_exceedsRiskCap_rejected() {
        User actor = new User();
        actor.setId(1L);
        actor.setRole(UserRole.INDIVIDUAL_USER);
        TradingAccount account = TradingAccount.builder().id(1L).buyingPowerUsd(new BigDecimal("100000")).user(actor).build();
        when(tradingAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(tradableAssetResolutionService.resolveEnabled(eq("AAPL"), any())).thenReturn(Optional.of(enabledStock()));
        TradingRiskProfile tight = TradingRiskProfile.builder()
                .archetype(TradingRiskArchetype.CONSERVATIVE)
                .maxSingleOrderUsd(new BigDecimal("100"))
                .dailyTradingCapUsd(new BigDecimal("100000"))
                .allowedAssetClasses(EnumSet.of(AssetClass.STOCK))
                .build();
        when(tradingRiskProfileService.getOrCreateDefault(actor)).thenReturn(tight);
        doNothing().when(tradingRiskProfileService).assertKycAllowsProfile(any(), any());
        doNothing().when(tradingRiskProfileService).assertAssetClassAllowed(any(), any());
        when(priceCacheService.get("AAPL")).thenReturn(new BigDecimal("150"));
        when(orderRepository.save(any())).thenAnswer(i -> {
            Order o = (Order) i.getArgument(0);
            o.setId(3L);
            return o;
        });

        assertThrows(TradingOrderFailedException.class, () ->
                orderExecutionService.placeOrder(1L, "AAPL", OrderSide.BUY, BigDecimal.TEN, null, actor));
    }

    @Test
    void placeOrder_accountNotFound_throws() {
        when(tradingAccountRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                orderExecutionService.placeOrder(99L, "AAPL", OrderSide.BUY, BigDecimal.ONE, null, new User()));
    }
}
