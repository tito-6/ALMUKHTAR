package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.annotation.RequireIdempotencyKey;
import com.mycompany.transfersystem.dto.wallet.CashOutResponse;
import com.mycompany.transfersystem.dto.wallet.WalletCashOutRequest;
import com.mycompany.transfersystem.entity.CashOutRequest;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.repository.CashOutRequestRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.wallet.WalletCashOutService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet/cashout")
@CrossOrigin(origins = "*")
public class WalletCashOutController {

    private final WalletCashOutService walletCashOutService;
    private final WalletService walletService;
    private final CashOutRequestRepository cashOutRequestRepository;
    private final UserRepository userRepository;

    public WalletCashOutController(WalletCashOutService walletCashOutService,
                                   WalletService walletService,
                                   CashOutRequestRepository cashOutRequestRepository,
                                   UserRepository userRepository) {
        this.walletCashOutService = walletCashOutService;
        this.walletService = walletService;
        this.cashOutRequestRepository = cashOutRequestRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/request")
    @RequireIdempotencyKey
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CashOutResponse> requestCashOut(
            @Valid @RequestBody WalletCashOutRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        CashOutRequest cashOut = walletCashOutService.requestCashOut(
                wallet.getId(), request.getBranchId(), request.getAmount(), request.getCurrency(), user);
        return ResponseEntity.ok(CashOutResponse.from(cashOut));
    }

    @PutMapping("/{id}/complete")
    @RequireIdempotencyKey
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER','CASHIER')")
    public ResponseEntity<CashOutResponse> completeCashOut(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        User cashier = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(CashOutResponse.from(walletCashOutService.completeCashOut(id, cashier)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER','CASHIER')")
    public ResponseEntity<List<CashOutResponse>> getPendingCashOuts(@RequestParam Long branchId) {
        return ResponseEntity.ok(cashOutRequestRepository.findByBranch_IdAndStatusOrderByCreatedAtAsc(branchId, "PENDING")
                .stream()
                .map(CashOutResponse::from)
                .toList());
    }
}
