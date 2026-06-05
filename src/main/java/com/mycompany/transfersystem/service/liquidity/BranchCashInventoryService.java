package com.mycompany.transfersystem.service.liquidity;

import com.mycompany.transfersystem.dto.liquidity.*;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.exception.ConditionNotMetException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Hawala office operations: vault balances, drawer ledger, inter-branch cash orders, and EOD reconciliation.
 */
@Service
@RequiredArgsConstructor
public class BranchCashInventoryService {

    private final BranchRepository branchRepository;
    private final BranchCashService branchCashService;
    private final BranchVaultBalanceRepository vaultBalanceRepository;
    private final CashierDrawerRepository cashierDrawerRepository;
    private final CashDrawerMovementRepository cashDrawerMovementRepository;
    private final CashTransferOrderRepository cashTransferOrderRepository;
    private final BranchCashInventoryRepository branchCashInventoryRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BranchVaultBalance> listVault(Long branchId) {
        return vaultBalanceRepository.findByBranch_Id(branchId);
    }

    @Transactional
    public BranchVaultBalance upsertVaultBalance(Long branchId, String currency, BigDecimal vaultBalance, User actor) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
        String c = normalizeCurrency(currency);
        BranchVaultBalance row = vaultBalanceRepository.findByBranch_IdAndCurrency(branchId, c)
                .orElseGet(() -> BranchVaultBalance.builder().branch(branch).currency(c).vaultBalance(BigDecimal.ZERO).build());
        row.setVaultBalance(money(vaultBalance));
        row.setLastReconciledAt(LocalDateTime.now());
        BranchVaultBalance saved = vaultBalanceRepository.save(row);
        auditService.log("VAULT_BALANCE_UPDATED", "BRANCH_VAULT", saved.getId(), c + "=" + saved.getVaultBalance(), actor);
        return saved;
    }

    @Transactional
    public CashDrawerMovement recordDrawerCashMovement(Long drawerId, CashDrawerMovement.MovementType type,
                                                       String currency, BigDecimal amount, User cashier,
                                                       Transaction tx, String note) {
        CashierDrawer drawer = cashierDrawerRepository.findById(drawerId)
                .orElseThrow(() -> new ResourceNotFoundException("Drawer not found: " + drawerId));
        if (drawer.getStatus() != CashierDrawer.DrawerStatus.OPEN) {
            throw new ConditionNotMetException("Drawer is not open");
        }
        CashDrawerMovement m = cashDrawerMovementRepository.save(CashDrawerMovement.builder()
                .drawer(drawer)
                .movementType(type)
                .currency(normalizeCurrency(currency))
                .amount(money(amount))
                .transaction(tx)
                .createdBy(cashier)
                .approvalStatus(CashDrawerMovement.ApprovalStatus.NONE)
                .note(note)
                .build());
        auditService.log("DRAWER_MOVEMENT", "CASH_DRAWER_MOVEMENT", m.getId(), type.name(), cashier);
        return m;
    }

    @Transactional
    public CashDrawerMovement requestDrawerAdjustment(Long drawerId, String currency, BigDecimal signedDelta,
                                                      User cashier, String note) {
        CashierDrawer drawer = cashierDrawerRepository.findById(drawerId)
                .orElseThrow(() -> new ResourceNotFoundException("Drawer not found: " + drawerId));
        if (drawer.getStatus() != CashierDrawer.DrawerStatus.OPEN) {
            throw new ConditionNotMetException("Drawer is not open");
        }
        CashDrawerMovement m = cashDrawerMovementRepository.save(CashDrawerMovement.builder()
                .drawer(drawer)
                .movementType(CashDrawerMovement.MovementType.ADJUSTMENT_REQUEST)
                .currency(normalizeCurrency(currency))
                .amount(money(signedDelta))
                .createdBy(cashier)
                .approvalStatus(CashDrawerMovement.ApprovalStatus.PENDING_MANAGER)
                .note(note)
                .build());
        auditService.log("DRAWER_ADJ_REQUEST", "CASH_DRAWER_MOVEMENT", m.getId(), currency + " " + signedDelta, cashier);
        Branch branch = drawer.getShift().getBranch();
        notificationService.notifyBranchOperation(branch.getId(), "Drawer adjustment pending",
                "Drawer #" + drawerId + " " + currency + " delta " + signedDelta);
        return m;
    }

    @Transactional
    public CashDrawerMovement approveDrawerAdjustment(Long movementId, User manager) {
        CashDrawerMovement m = cashDrawerMovementRepository.findById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException("Movement not found: " + movementId));
        if (m.getApprovalStatus() != CashDrawerMovement.ApprovalStatus.PENDING_MANAGER) {
            throw new ConditionNotMetException("Movement is not pending manager approval");
        }
        Branch branch = m.getDrawer().getShift().getBranch();
        branchCashService.applyAvailableDelta(branch, m.getCurrency(), m.getAmount(), manager,
                "Approved drawer adjustment #" + movementId);
        m.setApprovalStatus(CashDrawerMovement.ApprovalStatus.APPROVED);
        m.setApprovedBy(manager);
        m.setApprovedAt(LocalDateTime.now());
        cashDrawerMovementRepository.save(m);
        auditService.log("DRAWER_ADJ_APPROVED", "CASH_DRAWER_MOVEMENT", movementId, null, manager);
        return m;
    }

    @Transactional
    public CashDrawerMovement rejectDrawerAdjustment(Long movementId, User manager, String reason) {
        CashDrawerMovement m = cashDrawerMovementRepository.findById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException("Movement not found: " + movementId));
        if (m.getApprovalStatus() != CashDrawerMovement.ApprovalStatus.PENDING_MANAGER) {
            throw new ConditionNotMetException("Movement is not pending manager approval");
        }
        m.setApprovalStatus(CashDrawerMovement.ApprovalStatus.REJECTED);
        m.setApprovedBy(manager);
        m.setApprovedAt(LocalDateTime.now());
        m.setNote((m.getNote() != null ? m.getNote() + " | " : "") + "Rejected: " + reason);
        cashDrawerMovementRepository.save(m);
        auditService.log("DRAWER_ADJ_REJECTED", "CASH_DRAWER_MOVEMENT", movementId, reason, manager);
        return m;
    }

    @Transactional
    public CashTransferOrder createTransferOrder(CreateCashTransferOrderRequest req, User requester) {
        Branch from = branchRepository.findById(req.getFromBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("From branch not found"));
        Branch to = branchRepository.findById(req.getToBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("To branch not found"));
        if (from.getId().equals(to.getId())) {
            throw new ConditionNotMetException("Branches must differ");
        }
        CashTransferOrder order = cashTransferOrderRepository.save(CashTransferOrder.builder()
                .fromBranch(from)
                .toBranch(to)
                .currency(normalizeCurrency(req.getCurrency()))
                .amount(money(req.getAmount()))
                .status(CashTransferOrder.OrderStatus.REQUESTED)
                .requestedBy(requester)
                .notes(req.getNotes())
                .build());
        auditService.log("CASH_TRANSFER_REQUESTED", "CASH_TRANSFER_ORDER", order.getId(), null, requester);
        return order;
    }

    @Transactional
    public CashTransferOrder approveTransferOrder(Long orderId, User approver) {
        CashTransferOrder o = cashTransferOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (o.getStatus() != CashTransferOrder.OrderStatus.REQUESTED) {
            throw new ConditionNotMetException("Order is not in REQUESTED state");
        }
        o.setStatus(CashTransferOrder.OrderStatus.APPROVED);
        o.setApprovedBy(approver);
        o.setApprovedAt(LocalDateTime.now());
        auditService.log("CASH_TRANSFER_APPROVED", "CASH_TRANSFER_ORDER", orderId, null, approver);
        return cashTransferOrderRepository.save(o);
    }

    @Transactional
    public CashTransferOrder dispatchTransferOrder(Long orderId, User dispatcher) {
        CashTransferOrder o = cashTransferOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (o.getStatus() != CashTransferOrder.OrderStatus.APPROVED) {
            throw new ConditionNotMetException("Order must be APPROVED before dispatch");
        }
        branchCashService.recordInterBranchSend(o.getFromBranch(), o.getCurrency(), o.getAmount(), dispatcher,
                "Inter-branch order #" + orderId + " dispatched");
        o.setStatus(CashTransferOrder.OrderStatus.DISPATCHED);
        o.setDispatchedAt(LocalDateTime.now());
        auditService.log("CASH_TRANSFER_DISPATCHED", "CASH_TRANSFER_ORDER", orderId, null, dispatcher);
        return cashTransferOrderRepository.save(o);
    }

    @Transactional
    public CashTransferOrder receiveTransferOrder(Long orderId, User receiver) {
        CashTransferOrder o = cashTransferOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (o.getStatus() != CashTransferOrder.OrderStatus.DISPATCHED) {
            throw new ConditionNotMetException("Order must be DISPATCHED before receive");
        }
        branchCashService.recordInterBranchReceive(o.getToBranch(), o.getCurrency(), o.getAmount(), receiver,
                "Inter-branch order #" + orderId + " received");
        o.setStatus(CashTransferOrder.OrderStatus.RECEIVED);
        o.setReceivedAt(LocalDateTime.now());
        auditService.log("CASH_TRANSFER_RECEIVED", "CASH_TRANSFER_ORDER", orderId, null, receiver);
        return cashTransferOrderRepository.save(o);
    }

    @Transactional(readOnly = true)
    public List<CashTransferOrder> listTransferOrdersForBranch(Long branchId) {
        return cashTransferOrderRepository.findByFromBranch_IdOrToBranch_Id(branchId, branchId);
    }

    /**
     * End-of-day reconciliation: compares physical vault counts to ledger vault row and branch inventory to thresholds.
     */
    @Transactional
    public ReconciliationReport reconcileEndOfDay(Long branchId, User actor, Map<String, BigDecimal> physicalVaultByCurrency) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
        boolean mismatch = false;
        StringBuilder detail = new StringBuilder();
        for (Map.Entry<String, BigDecimal> e : physicalVaultByCurrency.entrySet()) {
            String c = normalizeCurrency(e.getKey());
            BigDecimal physical = money(e.getValue());
            BranchVaultBalance vault = vaultBalanceRepository.findByBranch_IdAndCurrency(branchId, c).orElse(null);
            BigDecimal systemVault = vault == null ? BigDecimal.ZERO : money(vault.getVaultBalance());
            if (systemVault.subtract(physical).abs().compareTo(new BigDecimal("0.0001")) > 0) {
                mismatch = true;
                detail.append(c).append(" vault system=").append(systemVault).append(" physical=").append(physical).append("; ");
            }
            if (vault != null) {
                vault.setVaultBalance(physical);
                vault.setLastReconciledAt(LocalDateTime.now());
                vaultBalanceRepository.save(vault);
            } else {
                vaultBalanceRepository.save(BranchVaultBalance.builder()
                        .branch(branch)
                        .currency(c)
                        .vaultBalance(physical)
                        .lastReconciledAt(LocalDateTime.now())
                        .build());
            }
        }
        for (BranchCashInventory inv : branchCashInventoryRepository.findByBranchId(branchId)) {
            if (inv.getLowCashThreshold() != null && inv.getLowCashThreshold().compareTo(BigDecimal.ZERO) > 0
                    && inv.getAvailableBalance().compareTo(inv.getLowCashThreshold()) <= 0) {
                mismatch = true;
                detail.append("Low cash ").append(inv.getCurrency()).append("; ");
            }
        }
        auditService.log("BRANCH_EOD_RECONCILED", "BRANCH", branchId, detail.toString(), actor);
        if (mismatch) {
            notificationService.notifyBranchOperation(branchId, "EOD cash mismatch or pressure",
                    "Branch " + branch.getName() + ": " + detail);
        }
        return ReconciliationReport.builder()
                .branchId(branchId)
                .mismatchDetected(mismatch)
                .detail(detail.toString())
                .reconciledAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CashDrawerMovement> listDrawerMovements(Long drawerId) {
        return cashDrawerMovementRepository.findByDrawerIdOrderByCreatedAtAsc(drawerId);
    }

    private static String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase();
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(4) : v.setScale(4, RoundingMode.HALF_UP);
    }
}
