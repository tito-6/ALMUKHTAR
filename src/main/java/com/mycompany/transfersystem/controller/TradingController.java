package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.trading.PlaceOrderRequest;
import com.mycompany.transfersystem.entity.Order;
import com.mycompany.transfersystem.entity.TradingAccount;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.trading.OrderExecutionService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trading")
@CrossOrigin(origins = "*")
public class TradingController {

    private final OrderExecutionService orderExecutionService;
    private final com.mycompany.transfersystem.repository.TradingAccountRepository tradingAccountRepository;
    private final com.mycompany.transfersystem.repository.OrderRepository orderRepository;
    private final com.mycompany.transfersystem.repository.PositionRepository positionRepository;
    private final UserRepository userRepository;

    public TradingController(OrderExecutionService orderExecutionService,
                             com.mycompany.transfersystem.repository.TradingAccountRepository tradingAccountRepository,
                             com.mycompany.transfersystem.repository.OrderRepository orderRepository,
                             com.mycompany.transfersystem.repository.PositionRepository positionRepository,
                             UserRepository userRepository) {
        this.orderExecutionService = orderExecutionService;
        this.tradingAccountRepository = tradingAccountRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.userRepository = userRepository;
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
}
