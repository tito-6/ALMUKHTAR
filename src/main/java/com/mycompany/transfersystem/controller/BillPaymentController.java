package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.annotation.RequireIdempotencyKey;
import com.mycompany.transfersystem.dto.bills.PayBillRequest;
import com.mycompany.transfersystem.entity.BillPaymentRequest;
import com.mycompany.transfersystem.entity.BillProvider;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.bills.BillPaymentService;
import com.mycompany.transfersystem.service.bills.BillProviderService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = "*")
public class BillPaymentController {

    private final BillProviderService billProviderService;
    private final BillPaymentService billPaymentService;
    private final UserRepository userRepository;

    public BillPaymentController(BillProviderService billProviderService,
                                 BillPaymentService billPaymentService,
                                 UserRepository userRepository) {
        this.billProviderService = billProviderService;
        this.billPaymentService = billPaymentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/providers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BillProvider>> getProviders(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(billProviderService.listByCategory(category));
    }

    @PostMapping("/pay")
    @RequireIdempotencyKey
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BillPaymentRequest> payBill(@Valid @RequestBody PayBillRequest request,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        BillPaymentRequest req = billPaymentService.payBill(
                request.getProviderId(),
                request.getAccountReference(),
                request.getAmount(),
                request.getCurrency() != null ? request.getCurrency() : "USD",
                user.getId(),
                user);
        return ResponseEntity.ok(req);
    }

    @GetMapping("/my-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BillPaymentRequest>> myHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(billPaymentService.getMyHistory(user.getId()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('CASHIER','BRANCH_MANAGER')")
    public ResponseEntity<List<BillPaymentRequest>> getPendingManual() {
        return ResponseEntity.ok(billPaymentService.getPendingManual());
    }

    @PutMapping("/admin/{id}/complete")
    @RequireIdempotencyKey
    @PreAuthorize("hasRole('CASHIER')")
    public ResponseEntity<BillPaymentRequest> completeManual(@PathVariable Long id,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        User cashier = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(billPaymentService.completeManualPayment(id, cashier));
    }
}
