package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.merchant.MerchantRegistrationRequest;
import com.mycompany.transfersystem.entity.Merchant;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.merchant.MerchantOnboardingService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
@CrossOrigin(origins = "*")
public class MerchantController {

    private final MerchantOnboardingService merchantOnboardingService;
    private final UserRepository userRepository;
    private final com.mycompany.transfersystem.repository.MerchantSettlementRepository settlementRepository;

    public MerchantController(MerchantOnboardingService merchantOnboardingService,
                              UserRepository userRepository,
                              com.mycompany.transfersystem.repository.MerchantSettlementRepository settlementRepository) {
        this.merchantOnboardingService = merchantOnboardingService;
        this.userRepository = userRepository;
        this.settlementRepository = settlementRepository;
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Merchant> register(@Valid @RequestBody MerchantRegistrationRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        Merchant m = merchantOnboardingService.register(
                user,
                request.getBusinessName(),
                request.getCategory(),
                request.getRegistrationNumber(),
                request.getAddress(),
                request.getCity(),
                request.getCountry(),
                request.getBranchId());
        return ResponseEntity.ok(m);
    }

    @GetMapping("/my-merchant")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Merchant> getMyMerchant(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        Merchant m = merchantOnboardingService.getByOwnerUserId(user.getId());
        return ResponseEntity.ok(m);
    }

    @GetMapping("/my-merchant/qr")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getMyMerchantQrData(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        Merchant m = merchantOnboardingService.getByOwnerUserId(user.getId());
        return ResponseEntity.ok(m.getQrCodeData() != null ? m.getQrCodeData() : "");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<List<Merchant>> listMerchants(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(merchantOnboardingService.listByStatus(status));
    }

    @PutMapping("/admin/{id}/approve")
    @PreAuthorize("hasRole('MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Merchant> approve(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(merchantOnboardingService.approve(id, admin));
    }

    @GetMapping("/admin/settlements/pending")
    @PreAuthorize("hasRole('MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<List<com.mycompany.transfersystem.entity.MerchantSettlement>> getPendingSettlements() {
        return ResponseEntity.ok(settlementRepository.findByStatus("PENDING"));
    }
}
