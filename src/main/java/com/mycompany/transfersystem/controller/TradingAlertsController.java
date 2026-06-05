package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.trading.CreatePriceAlertRequest;
import com.mycompany.transfersystem.entity.PriceAlert;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.trading.PriceAlertService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trading/alerts")
@CrossOrigin(origins = "*")
public class TradingAlertsController {

    private final PriceAlertService priceAlertService;
    private final UserRepository userRepository;

    public TradingAlertsController(PriceAlertService priceAlertService, UserRepository userRepository) {
        this.priceAlertService = priceAlertService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PriceAlert> create(
            @Valid @RequestBody CreatePriceAlertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        PriceAlert a = priceAlertService.create(
                user,
                request.getSymbol(),
                request.getKind(),
                request.getThresholdPrice(),
                request.getThresholdPercent(),
                request.isNotifyInApp(),
                request.isNotifyWhatsApp());
        return ResponseEntity.ok(a);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PriceAlert>> list(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(priceAlertService.listForUser(user.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        priceAlertService.deleteOwned(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
