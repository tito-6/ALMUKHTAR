package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.wallet.TopupRequestDto;
import com.mycompany.transfersystem.entity.TopupRequest;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.wallet.WalletService;
import com.mycompany.transfersystem.service.wallet.WalletTopUpService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet/topup")
@CrossOrigin(origins = "*")
public class WalletTopUpController {

    private final WalletTopUpService walletTopUpService;
    private final WalletService walletService;
    private final UserRepository userRepository;

    public WalletTopUpController(WalletTopUpService walletTopUpService,
                                 WalletService walletService,
                                 UserRepository userRepository) {
        this.walletTopUpService = walletTopUpService;
        this.walletService = walletService;
        this.userRepository = userRepository;
    }

    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TopupRequest> requestTopUp(
            @Valid @RequestBody TopupRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        TopupRequest req = walletTopUpService.requestTopUp(wallet.getId(), request.getBranchId(),
                request.getAmount(), request.getCurrency(), user);
        return ResponseEntity.ok(req);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER','CASHIER')")
    public ResponseEntity<TopupRequest> completeTopUp(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User cashier = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(walletTopUpService.completeTopUp(id, cashier));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER','CASHIER')")
    public ResponseEntity<List<TopupRequest>> getPendingTopUps(@RequestParam Long branchId) {
        return ResponseEntity.ok(walletTopUpService.getPendingTopUps(branchId));
    }
}
