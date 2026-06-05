package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.cashier.CloseShiftRequest;
import com.mycompany.transfersystem.dto.cashier.OpenShiftRequest;
import com.mycompany.transfersystem.dto.liquidity.CreateCashTransferOrderRequest;
import com.mycompany.transfersystem.dto.liquidity.ReconciliationReport;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.liquidity.BranchCashInventoryService;
import com.mycompany.transfersystem.service.liquidity.BranchCashService;
import com.mycompany.transfersystem.service.liquidity.CashierShiftService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/branch-cash")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BranchCashController {

    private final BranchCashService branchCashService;
    private final BranchCashInventoryService branchCashInventoryService;
    private final CashierShiftService hawalaCashierShiftService;
    private final UserRepository userRepository;

    @GetMapping("/branches/{branchId}/vault")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','PLATFORM_OWNER','AUDITOR')")
    public ResponseEntity<List<BranchVaultBalance>> vault(@PathVariable Long branchId) {
        return ResponseEntity.ok(branchCashInventoryService.listVault(branchId));
    }

    @PutMapping("/branches/{branchId}/vault/{currency}")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<BranchVaultBalance> putVault(@PathVariable Long branchId,
                                                     @PathVariable String currency,
                                                     @RequestParam BigDecimal balance,
                                                     @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(branchCashInventoryService.upsertVaultBalance(branchId, currency, balance, u));
    }

    @GetMapping("/branches/{branchId}/inventory")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','PLATFORM_OWNER','AUDITOR')")
    public ResponseEntity<List<BranchCashInventory>> inventory(@PathVariable Long branchId) {
        return ResponseEntity.ok(branchCashService.getInventory(branchId));
    }

    @PostMapping("/cashier/shifts/open-with-drawer")
    @PreAuthorize("hasAnyRole('CASHIER','BRANCH_MANAGER')")
    public ResponseEntity<CashierShift> openWithDrawer(@Valid @RequestBody OpenShiftRequest req,
                                                       @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(hawalaCashierShiftService.openShiftWithDrawer(req, u));
    }

    @PostMapping("/cashier/shifts/close-with-drawer")
    @PreAuthorize("hasAnyRole('CASHIER','BRANCH_MANAGER')")
    public ResponseEntity<CashierShift> closeWithDrawer(@Valid @RequestBody CloseShiftRequest req,
                                                       @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(hawalaCashierShiftService.closeShiftWithDrawer(req, u));
    }

    @GetMapping("/cashier/shifts/{shiftId}/drawer")
    @PreAuthorize("hasAnyRole('CASHIER','BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','AUDITOR')")
    public ResponseEntity<CashierDrawer> drawer(@PathVariable Long shiftId) {
        return ResponseEntity.ok(hawalaCashierShiftService.getDrawerForShift(shiftId));
    }

    @GetMapping("/drawers/{drawerId}/movements")
    @PreAuthorize("hasAnyRole('CASHIER','BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','AUDITOR')")
    public ResponseEntity<List<CashDrawerMovement>> drawerMovements(@PathVariable Long drawerId) {
        return ResponseEntity.ok(branchCashInventoryService.listDrawerMovements(drawerId));
    }

    @PostMapping("/drawers/{drawerId}/adjustments")
    @PreAuthorize("hasAnyRole('CASHIER')")
    public ResponseEntity<CashDrawerMovement> requestAdjustment(@PathVariable Long drawerId,
                                                                @RequestParam String currency,
                                                                @RequestParam BigDecimal signedDelta,
                                                                @RequestParam(required = false) String note,
                                                                @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(branchCashInventoryService.requestDrawerAdjustment(drawerId, currency, signedDelta, u, note));
    }

    @PostMapping("/drawer-movements/{movementId}/approve")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<CashDrawerMovement> approveAdjustment(@PathVariable Long movementId,
                                                              @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(branchCashInventoryService.approveDrawerAdjustment(movementId, u));
    }

    @PostMapping("/drawer-movements/{movementId}/reject")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<CashDrawerMovement> rejectAdjustment(@PathVariable Long movementId,
                                                               @RequestParam(required = false) String reason,
                                                               @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(branchCashInventoryService.rejectDrawerAdjustment(movementId, u, reason == null ? "" : reason));
    }

    @PostMapping("/transfer-orders")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<CashTransferOrder> createOrder(@Valid @RequestBody CreateCashTransferOrderRequest req,
                                                         @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(branchCashInventoryService.createTransferOrder(req, u));
    }

    @PostMapping("/transfer-orders/{id}/approve")
    @PreAuthorize("hasAnyRole('MOTHER_BRANCH_ADMIN','PLATFORM_OWNER')")
    public ResponseEntity<CashTransferOrder> approveOrder(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(branchCashInventoryService.approveTransferOrder(id, u));
    }

    @PostMapping("/transfer-orders/{id}/dispatch")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<CashTransferOrder> dispatch(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(branchCashInventoryService.dispatchTransferOrder(id, u));
    }

    @PostMapping("/transfer-orders/{id}/receive")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<CashTransferOrder> receive(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(branchCashInventoryService.receiveTransferOrder(id, u));
    }

    @GetMapping("/branches/{branchId}/transfer-orders")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','AUDITOR')")
    public ResponseEntity<List<CashTransferOrder>> orders(@PathVariable Long branchId) {
        return ResponseEntity.ok(branchCashInventoryService.listTransferOrdersForBranch(branchId));
    }

    @PostMapping("/branches/{branchId}/reconcile-eod")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<ReconciliationReport> reconcile(@PathVariable Long branchId,
                                                          @RequestBody Map<String, BigDecimal> physicalVault,
                                                          @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(branchCashInventoryService.reconcileEndOfDay(branchId, u, physicalVault));
    }
}
