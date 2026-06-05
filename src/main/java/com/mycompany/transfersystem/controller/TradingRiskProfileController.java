package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.trading.UpdateTradingRiskProfileRequest;
import com.mycompany.transfersystem.entity.TradingRiskProfile;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.trading.TradingRiskProfileService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trading/risk-profile")
@CrossOrigin(origins = "*")
public class TradingRiskProfileController {

    private final TradingRiskProfileService tradingRiskProfileService;
    private final UserRepository userRepository;

    public TradingRiskProfileController(TradingRiskProfileService tradingRiskProfileService,
                                        UserRepository userRepository) {
        this.tradingRiskProfileService = tradingRiskProfileService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TradingRiskProfile> get(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(tradingRiskProfileService.getOrCreateDefault(user));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TradingRiskProfile> update(
            @Valid @RequestBody UpdateTradingRiskProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        TradingRiskProfile p = tradingRiskProfileService.updateArchetype(user, request.getArchetype());
        return ResponseEntity.ok(p);
    }
}
