package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.annotation.RequireIdempotencyKey;
import com.mycompany.transfersystem.dto.wallet.WalletExchangeRequest;
import com.mycompany.transfersystem.dto.wallet.WalletResponse;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.WalletBalance;
import com.mycompany.transfersystem.entity.WalletTransaction;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.wallet.WalletService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;

    public WalletController(WalletService walletService, UserRepository userRepository) {
        this.walletService = walletService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-wallet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WalletResponse> getMyWallet(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        List<WalletBalance> balances = walletService.getBalances(wallet.getId());
        WalletResponse res = new WalletResponse();
        res.setId(wallet.getId());
        res.setUserId(wallet.getUser().getId());
        res.setWalletNumber(wallet.getWalletNumber());
        res.setStatus(wallet.getStatus());
        res.setKycTier(wallet.getKycTier());
        res.setDailyLimit(wallet.getDailyLimit());
        res.setMonthlyLimit(wallet.getMonthlyLimit());
        res.setCreatedAt(wallet.getCreatedAt());
        res.setBalances(balances.stream().map(b -> {
            WalletResponse.WalletBalanceDto dto = new WalletResponse.WalletBalanceDto();
            dto.setCurrencyCode(b.getCurrencyCode());
            dto.setAvailableBalance(b.getAvailableBalance());
            dto.setLockedBalance(b.getLockedBalance());
            return dto;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/my-wallet/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<WalletTransaction>> getTransactionHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        return ResponseEntity.ok(walletService.getTransactionHistory(wallet.getId(), pageable));
    }

    @PostMapping("/exchange")
    @RequireIdempotencyKey
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> exchange(
            @Valid @RequestBody WalletExchangeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        walletService.exchangeCurrency(wallet.getId(), request.getFromCurrency(), request.getToCurrency(),
                request.getAmount(), user);
        return ResponseEntity.ok().build();
    }
}
