package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.*;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.CorporateAccountService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/corporate")
@CrossOrigin(origins = "*")
public class CorporateAccountController {

    private final CorporateAccountService corporateService;
    private final UserRepository userRepository;

    public CorporateAccountController(CorporateAccountService corporateService,
                                     UserRepository userRepository) {
        this.corporateService = corporateService;
        this.userRepository = userRepository;
    }

    @PostMapping("/sub-accounts")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<CorporateAccountResponse> createSubAccount(
            @Valid @RequestBody CreateSubAccountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(corporateService.createSubAccount(request, user));
    }

    @GetMapping("/{parentUserId}/sub-accounts")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<Page<CorporateAccountResponse>> listSubAccounts(
            @PathVariable Long parentUserId, Pageable pageable) {
        return ResponseEntity.ok(corporateService.listSubAccounts(parentUserId, pageable));
    }

    @PostMapping("/payroll/{parentUserId}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<PayrollBatchResponse> processPayroll(
            @PathVariable Long parentUserId,
            @Valid @RequestBody PayrollBatchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(corporateService.processPayrollBatch(parentUserId, request, user));
    }

    @GetMapping("/{parentUserId}/ledger-summary")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER','AUDITOR')")
    public ResponseEntity<LedgerSummaryResponse> getLedgerSummary(@PathVariable Long parentUserId) {
        return ResponseEntity.ok(corporateService.getLedgerSummary(parentUserId));
    }
}
