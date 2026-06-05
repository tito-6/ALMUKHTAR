package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.cashier.CloseShiftRequest;
import com.mycompany.transfersystem.dto.cashier.OpenShiftRequest;
import com.mycompany.transfersystem.entity.CashierShift;
import com.mycompany.transfersystem.entity.CashierShiftBalance;
import com.mycompany.transfersystem.entity.CashierShiftEntry;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.cashier.CashierShiftService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cashier/shifts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CashierShiftController {

    private final CashierShiftService cashierShiftService;
    private final UserRepository userRepository;

    @PostMapping("/open")
    @PreAuthorize("hasRole('CASHIER')")
    public ResponseEntity<CashierShift> open(@Valid @RequestBody OpenShiftRequest request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        User cashier = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(cashierShiftService.openShift(request, cashier));
    }

    @PostMapping("/close")
    @PreAuthorize("hasRole('CASHIER')")
    public ResponseEntity<CashierShift> close(@Valid @RequestBody CloseShiftRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        User cashier = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(cashierShiftService.closeShift(request, cashier));
    }

    @PostMapping("/{shiftId}/approve")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','PLATFORM_OWNER')")
    public ResponseEntity<CashierShift> approve(@PathVariable Long shiftId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        User approver = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(cashierShiftService.approveShift(shiftId, approver));
    }

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','PLATFORM_OWNER','AUDITOR')")
    public ResponseEntity<List<CashierShift>> openShifts(@RequestParam Long branchId) {
        return ResponseEntity.ok(cashierShiftService.getOpenShifts(branchId));
    }

    @GetMapping("/{shiftId}/balances")
    @PreAuthorize("hasAnyRole('CASHIER','BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','PLATFORM_OWNER','AUDITOR')")
    public ResponseEntity<List<CashierShiftBalance>> balances(@PathVariable Long shiftId) {
        return ResponseEntity.ok(cashierShiftService.getShiftBalances(shiftId));
    }

    @GetMapping("/{shiftId}/entries")
    @PreAuthorize("hasAnyRole('CASHIER','BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','PLATFORM_OWNER','AUDITOR')")
    public ResponseEntity<List<CashierShiftEntry>> entries(@PathVariable Long shiftId) {
        return ResponseEntity.ok(cashierShiftService.getShiftEntries(shiftId));
    }
}
