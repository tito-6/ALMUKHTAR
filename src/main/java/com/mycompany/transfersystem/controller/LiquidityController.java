package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.liquidity.BranchCashAdjustmentRequest;
import com.mycompany.transfersystem.entity.BranchCashInventory;
import com.mycompany.transfersystem.entity.LiquidityAlert;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.liquidity.BranchCashService;
import com.mycompany.transfersystem.service.liquidity.LiquidityForecastService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/liquidity")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LiquidityController {

    private final LiquidityForecastService liquidityForecastService;
    private final BranchCashService branchCashService;
    private final UserRepository userRepository;

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('MOTHER_BRANCH_ADMIN','BRANCH_MANAGER','PLATFORM_OWNER')")
    public ResponseEntity<List<LiquidityAlert>> alerts() {
        return ResponseEntity.ok(liquidityForecastService.getUnresolvedAlerts());
    }

    @PostMapping("/alerts/{id}/resolve")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Void> resolve(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        liquidityForecastService.resolveAlert(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/forecast/{branchId}")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> forecast(@PathVariable Long branchId) {
        return ResponseEntity.ok(liquidityForecastService.getForecast(branchId));
    }

    @GetMapping("/branches/{branchId}/cash")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','PLATFORM_OWNER','AUDITOR')")
    public ResponseEntity<List<BranchCashInventory>> branchCash(@PathVariable Long branchId) {
        return ResponseEntity.ok(branchCashService.getInventory(branchId));
    }

    @PostMapping("/branches/cash-adjustments")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','PLATFORM_OWNER')")
    public ResponseEntity<BranchCashInventory> adjustCash(@Valid @RequestBody BranchCashAdjustmentRequest request,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(branchCashService.adjustCashCount(request, user));
    }
}
