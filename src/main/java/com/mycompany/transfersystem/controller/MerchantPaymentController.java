package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.annotation.RequireIdempotencyKey;
import com.mycompany.transfersystem.dto.merchant.MerchantPaymentRequest;
import com.mycompany.transfersystem.entity.MerchantTransaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.merchant.MerchantPaymentService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchants")
@CrossOrigin(origins = "*")
public class MerchantPaymentController {

    private final MerchantPaymentService merchantPaymentService;
    private final UserRepository userRepository;

    public MerchantPaymentController(MerchantPaymentService merchantPaymentService,
                                     UserRepository userRepository) {
        this.merchantPaymentService = merchantPaymentService;
        this.userRepository = userRepository;
    }

    @PostMapping("/pay")
    @RequireIdempotencyKey
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MerchantTransaction> pay(@Valid @RequestBody MerchantPaymentRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        MerchantTransaction tx;
        if (request.getMerchantQrData() != null && !request.getMerchantQrData().isBlank()) {
            tx = merchantPaymentService.processQrPayment(
                    request.getMerchantQrData(),
                    user.getId(),
                    request.getAmount(),
                    request.getDescription(),
                    user);
        } else if (request.getMerchantId() != null) {
            tx = merchantPaymentService.processPayment(
                    request.getMerchantId(),
                    user.getId(),
                    request.getAmount(),
                    request.getCurrency() != null ? request.getCurrency() : "USD",
                    request.getDescription(),
                    null,
                    user);
        } else {
            throw new IllegalArgumentException("Either merchantQrData or merchantId must be provided");
        }
        return ResponseEntity.ok(tx);
    }
}
