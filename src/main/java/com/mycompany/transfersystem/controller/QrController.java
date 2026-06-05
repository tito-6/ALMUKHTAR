package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.annotation.RequireIdempotencyKey;
import com.mycompany.transfersystem.dto.QrGenerationResponse;
import com.mycompany.transfersystem.dto.ScanValidationRequest;
import com.mycompany.transfersystem.dto.ScanValidationResponse;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.ratelimit.RateLimitService;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.BarcodeGenerationService;
import com.mycompany.transfersystem.service.ScannerValidationService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr")
@CrossOrigin(origins = "*")
public class QrController {

    private final BarcodeGenerationService barcodeService;
    private final ScannerValidationService scannerService;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;

    public QrController(BarcodeGenerationService barcodeService,
                       ScannerValidationService scannerService,
                       UserRepository userRepository,
                       RateLimitService rateLimitService) {
        this.barcodeService = barcodeService;
        this.scannerService = scannerService;
        this.userRepository = userRepository;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/generate/{transactionId}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','BRANCH_MANAGER','CASHIER')")
    public ResponseEntity<QrGenerationResponse> generate(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(barcodeService.generateForTransaction(transactionId, user));
    }

    @PostMapping("/scan")
    @RequireIdempotencyKey
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','BRANCH_MANAGER','CASHIER')")
    public ResponseEntity<?> scan(
            @Valid @RequestBody ScanValidationRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        String deviceId = httpRequest.getHeader("X-Device-ID") != null
                ? httpRequest.getHeader("X-Device-ID") : httpRequest.getRemoteAddr();
        if (!rateLimitService.tryConsume("qr-scan:" + deviceId, 10)) {
            return ResponseEntity.status(429).body("Too many requests");
        }
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(scannerService.validateScan(request, user));
    }
}
