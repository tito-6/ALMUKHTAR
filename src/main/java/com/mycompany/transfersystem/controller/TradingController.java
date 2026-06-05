package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.annotation.RequireIdempotencyKey;
import com.mycompany.transfersystem.dto.trading.UserTradingNotificationPreferencesDto;
import com.mycompany.transfersystem.dto.trading.PlaceOrderRequest;
import com.mycompany.transfersystem.entity.Order;
import com.mycompany.transfersystem.entity.TradingAccount;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.UserTradingNotificationPreferences;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.repository.UserTradingNotificationPreferencesRepository;
import com.mycompany.transfersystem.service.trading.CorporateTreasuryDashboardService;
import com.mycompany.transfersystem.service.trading.OrderExecutionService;
import com.mycompany.transfersystem.service.trading.PortfolioRiskAnalyticsService;
import com.mycompany.transfersystem.service.trading.TradingRiskProfileService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trading")
@CrossOrigin(origins = "*")
public class TradingController {

    private final OrderExecutionService orderExecutionService;
    private final com.mycompany.transfersystem.repository.TradingAccountRepository tradingAccountRepository;
    private final com.mycompany.transfersystem.repository.OrderRepository orderRepository;
    private final com.mycompany.transfersystem.repository.PositionRepository positionRepository;
    private final UserRepository userRepository;
    private final TradingRiskProfileService tradingRiskProfileService;
    private final PortfolioRiskAnalyticsService portfolioRiskAnalyticsService;
    private final CorporateTreasuryDashboardService corporateTreasuryDashboardService;
    private final UserTradingNotificationPreferencesRepository tradingNotificationPreferencesRepository;

    public TradingController(OrderExecutionService orderExecutionService,
                             com.mycompany.transfersystem.repository.TradingAccountRepository tradingAccountRepository,
                             com.mycompany.transfersystem.repository.OrderRepository orderRepository,
                             com.mycompany.transfersystem.repository.PositionRepository positionRepository,
                             UserRepository userRepository,
                             TradingRiskProfileService tradingRiskProfileService,
                             PortfolioRiskAnalyticsService portfolioRiskAnalyticsService,
                             CorporateTreasuryDashboardService corporateTreasuryDashboardService,
                             UserTradingNotificationPreferencesRepository tradingNotificationPreferencesRepository) {
        this.orderExecutionService = orderExecutionService;
        this.tradingAccountRepository = tradingAccountRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.userRepository = userRepository;
        this.tradingRiskProfileService = tradingRiskProfileService;
        this.portfolioRiskAnalyticsService = portfolioRiskAnalyticsService;
        this.corporateTreasuryDashboardService = corporateTreasuryDashboardService;
        this.tradingNotificationPreferencesRepository = tradingNotificationPreferencesRepository;
    }

    @PostMapping("/account/open")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TradingAccount> openAccount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        TradingAccount account = tradingAccountRepository.findByUser_Id(user.getId()).orElse(null);
        if (account != null) {
            return ResponseEntity.ok(account);
        }
        account = TradingAccount.builder()
                .user(user)
                .status("ACTIVE")
                .buyingPowerUsd(java.math.BigDecimal.ZERO)
                .totalPortfolioValue(java.math.BigDecimal.ZERO)
                .build();
        account = tradingAccountRepository.save(account);
        tradingRiskProfileService.getOrCreateDefault(user);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TradingAccount> getAccount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        TradingAccount account = tradingAccountRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new com.mycompany.transfersystem.exception.ResourceNotFoundException("Trading account not found"));
        return ResponseEntity.ok(account);
    }

    @PostMapping("/orders")
    @RequireIdempotencyKey
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        TradingAccount account = tradingAccountRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new com.mycompany.transfersystem.exception.ResourceNotFoundException("Open a trading account first"));
        Order order = orderExecutionService.placeOrder(account.getId(), request.getSymbol(), request.getSide(),
                request.getQuantity(), request.getLimitPrice(), user);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Order>> getOrders(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        TradingAccount account = tradingAccountRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new com.mycompany.transfersystem.exception.ResourceNotFoundException("Trading account not found"));
        return ResponseEntity.ok(orderRepository.findByTradingAccount_IdAndStatus(account.getId(), com.mycompany.transfersystem.entity.enums.OrderStatus.PENDING));
    }

    @GetMapping("/positions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<com.mycompany.transfersystem.entity.Position>> getPositions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        TradingAccount account = tradingAccountRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new com.mycompany.transfersystem.exception.ResourceNotFoundException("Trading account not found"));
        return ResponseEntity.ok(positionRepository.findByTradingAccount_Id(account.getId()));
    }

    @GetMapping("/portfolio/risk")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> portfolioRisk(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(portfolioRiskAnalyticsService.buildRiskSnapshot(user.getId()));
    }

    @GetMapping("/corporate/treasury-dashboard")
    @PreAuthorize("hasRole('CORPORATE_ADMIN')")
    public ResponseEntity<Map<String, Object>> corporateTreasury(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(corporateTreasuryDashboardService.buildDashboard(user.getId()));
    }

    @GetMapping("/notification-preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserTradingNotificationPreferencesDto> getNotifPrefs(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        UserTradingNotificationPreferences p = tradingNotificationPreferencesRepository.findByUserId(user.getId())
                .orElse(UserTradingNotificationPreferences.builder()
                        .userId(user.getId())
                        .build());
        UserTradingNotificationPreferencesDto dto = new UserTradingNotificationPreferencesDto();
        dto.setOrderInApp(p.isOrderInApp());
        dto.setOrderWhatsApp(p.isOrderWhatsApp());
        dto.setPriceAlertInApp(p.isPriceAlertInApp());
        dto.setPriceAlertWhatsApp(p.isPriceAlertWhatsApp());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/notification-preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserTradingNotificationPreferencesDto> putNotifPrefs(
            @RequestBody UserTradingNotificationPreferencesDto body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        UserTradingNotificationPreferences p = tradingNotificationPreferencesRepository.findByUserId(user.getId())
                .orElse(UserTradingNotificationPreferences.builder().userId(user.getId()).build());
        p.setOrderInApp(body.isOrderInApp());
        p.setOrderWhatsApp(body.isOrderWhatsApp());
        p.setPriceAlertInApp(body.isPriceAlertInApp());
        p.setPriceAlertWhatsApp(body.isPriceAlertWhatsApp());
        tradingNotificationPreferencesRepository.save(p);
        return ResponseEntity.ok(body);
    }
}
