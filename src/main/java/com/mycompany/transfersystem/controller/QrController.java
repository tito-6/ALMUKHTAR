package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.QrGenerationResponse;
import com.mycompany.transfersystem.dto.ScanValidationRequest;
import com.mycompany.transfersystem.dto.ScanValidationResponse;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.BarcodeGenerationService;
import com.mycompany.transfersystem.service.ScannerValidationService;
import com.mycompany.transfersystem.util.SecurityUtils;
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

    public QrController(BarcodeGenerationService barcodeService,
                       ScannerValidationService scannerService,
                       UserRepository userRepository) {
        this.barcodeService = barcodeService;
        this.scannerService = scannerService;
        this.userRepository = userRepository;
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
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','BRANCH_MANAGER','CASHIER')")
    public ResponseEntity<ScanValidationResponse> scan(
            @Valid @RequestBody ScanValidationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(scannerService.validateScan(request, user));
    }
}
