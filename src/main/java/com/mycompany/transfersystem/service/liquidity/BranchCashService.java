package com.mycompany.transfersystem.service.liquidity;

import com.mycompany.transfersystem.dto.liquidity.BranchCashAdjustmentRequest;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.exception.InsufficientFundsException;
import com.mycompany.transfersystem.exception.InvalidTransactionException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.cashier.CashierShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchCashService {

    private final BranchRepository branchRepository;
    private final BranchCashInventoryRepository inventoryRepository;
    private final BranchCashReservationRepository reservationRepository;
    private final BranchCashMovementRepository movementRepository;
    private final AuditService auditService;
    private final CashierShiftService cashierShiftService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(readOnly = true)
    public List<BranchCashInventory> getInventory(Long branchId) {
        return inventoryRepository.findByBranchId(branchId);
    }

    @Transactional
    public BranchCashInventory adjustCashCount(BranchCashAdjustmentRequest request, User cashier) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));
        String currency = normalizeCurrency(request.getCurrency());
        BranchCashInventory inventory = inventoryRepository
                .findByBranchIdAndCurrencyForUpdate(branch.getId(), currency)
                .orElseGet(() -> BranchCashInventory.builder()
                        .branch(branch)
                        .currency(currency)
                        .availableBalance(BigDecimal.ZERO)
                        .reservedBalance(BigDecimal.ZERO)
                        .lowCashThreshold(BigDecimal.ZERO)
                        .highCashThreshold(BigDecimal.ZERO)
                        .build());

        inventory.setAvailableBalance(money(request.getAvailableBalance()));
        if (request.getLowCashThreshold() != null) {
            inventory.setLowCashThreshold(money(request.getLowCashThreshold()));
        }
        if (request.getHighCashThreshold() != null) {
            inventory.setHighCashThreshold(money(request.getHighCashThreshold()));
        }
        BranchCashInventory saved = inventoryRepository.save(inventory);
        recordMovement(branch, null, cashier, currency, BranchCashMovement.MovementType.CASH_COUNT_ADJUSTMENT,
                saved.getAvailableBalance(), saved, request.getNote());
        auditService.log("BRANCH_CASH_COUNT_ADJUSTED", "BRANCH_CASH_INVENTORY", saved.getId(),
                currency + " available=" + saved.getAvailableBalance(), cashier);
        return saved;
    }

    @Transactional
    public BranchCashReservation reservePayout(Transaction transaction, Branch payoutBranch,
                                               String currency, BigDecimal amount, User actor) {
        String normalizedCurrency = normalizeCurrency(currency);
        BigDecimal payoutAmount = money(amount);
        return reservationRepository.findByTransactionIdForUpdate(transaction.getId())
                .orElseGet(() -> createReservation(transaction, payoutBranch, normalizedCurrency, payoutAmount, actor));
    }

    @Transactional
    public void recordCashIn(Branch branch, String currency, BigDecimal amount, User cashier,
                             String referenceType, Long referenceId, String note) {
        String normalizedCurrency = normalizeCurrency(currency);
        BigDecimal cashAmount = money(amount);
        BranchCashInventory inventory = inventoryRepository
                .findByBranchIdAndCurrencyForUpdate(branch.getId(), normalizedCurrency)
                .orElseGet(() -> BranchCashInventory.builder()
                        .branch(branch)
                        .currency(normalizedCurrency)
                        .availableBalance(BigDecimal.ZERO)
                        .reservedBalance(BigDecimal.ZERO)
                        .lowCashThreshold(BigDecimal.ZERO)
                        .highCashThreshold(BigDecimal.ZERO)
                        .build());
        inventory.setAvailableBalance(money(inventory.getAvailableBalance().add(cashAmount)));
        inventoryRepository.save(inventory);
        recordMovement(branch, null, cashier, normalizedCurrency, BranchCashMovement.MovementType.CASH_IN_RECEIVED,
                cashAmount, inventory, note);
        cashierShiftService.recordCashIn(cashier, branch, normalizedCurrency, cashAmount, referenceType, referenceId, note);
        auditService.log("BRANCH_CASH_IN_RECEIVED", "BRANCH_CASH_INVENTORY", inventory.getId(),
                normalizedCurrency + " " + cashAmount, cashier);
    }

    /**
     * Applies a signed delta to branch available cash and writes an auditable movement row.
     */
    @Transactional
    public BranchCashInventory applyAvailableDelta(Branch branch, String currency, BigDecimal signedDelta,
                                                   User actor, String note) {
        String normalizedCurrency = normalizeCurrency(currency);
        BigDecimal delta = money(signedDelta);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return inventoryRepository.findByBranchIdAndCurrency(branch.getId(), normalizedCurrency)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch cash inventory not found"));
        }
        BranchCashInventory inventory = inventoryRepository
                .findByBranchIdAndCurrencyForUpdate(branch.getId(), normalizedCurrency)
                .orElseGet(() -> BranchCashInventory.builder()
                        .branch(branch)
                        .currency(normalizedCurrency)
                        .availableBalance(BigDecimal.ZERO)
                        .reservedBalance(BigDecimal.ZERO)
                        .lowCashThreshold(BigDecimal.ZERO)
                        .highCashThreshold(BigDecimal.ZERO)
                        .build());
        BigDecimal next = money(inventory.getAvailableBalance().add(delta));
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Adjustment would make available balance negative for "
                    + normalizedCurrency + " at branch " + branch.getId());
        }
        inventory.setAvailableBalance(next);
        inventoryRepository.save(inventory);
        recordMovement(branch, null, actor, normalizedCurrency,
                BranchCashMovement.MovementType.MANAGER_APPROVED_DRAWER_ADJUSTMENT,
                delta.abs(), inventory, note != null ? note : "Drawer adjustment");
        auditService.log("BRANCH_CASH_LEDGER_DELTA", "BRANCH_CASH_INVENTORY", inventory.getId(),
                normalizedCurrency + " delta=" + delta, actor);
        return inventory;
    }

    @Transactional
    public void recordInterBranchSend(Branch fromBranch, String currency, BigDecimal amount, User actor, String note) {
        String c = normalizeCurrency(currency);
        BigDecimal amt = money(amount);
        BranchCashInventory inv = inventoryRepository
                .findByBranchIdAndCurrencyForUpdate(fromBranch.getId(), c)
                .orElseThrow(() -> new ResourceNotFoundException("Branch cash inventory not found"));
        if (inv.getAvailableBalance().compareTo(amt) < 0) {
            throw new InsufficientFundsException("Insufficient available cash for inter-branch transfer");
        }
        inv.setAvailableBalance(money(inv.getAvailableBalance().subtract(amt)));
        inventoryRepository.save(inv);
        recordMovement(fromBranch, null, actor, c, BranchCashMovement.MovementType.INTER_BRANCH_CASH_SENT,
                amt, inv, note);
        auditService.log("INTER_BRANCH_CASH_SENT", "BRANCH_CASH_INVENTORY", inv.getId(), c + " " + amt, actor);
    }

    @Transactional
    public void recordInterBranchReceive(Branch toBranch, String currency, BigDecimal amount, User actor, String note) {
        String c = normalizeCurrency(currency);
        BigDecimal amt = money(amount);
        BranchCashInventory inv = inventoryRepository
                .findByBranchIdAndCurrencyForUpdate(toBranch.getId(), c)
                .orElseGet(() -> BranchCashInventory.builder()
                        .branch(toBranch)
                        .currency(c)
                        .availableBalance(BigDecimal.ZERO)
                        .reservedBalance(BigDecimal.ZERO)
                        .lowCashThreshold(BigDecimal.ZERO)
                        .highCashThreshold(BigDecimal.ZERO)
                        .build());
        inv.setAvailableBalance(money(inv.getAvailableBalance().add(amt)));
        inventoryRepository.save(inv);
        recordMovement(toBranch, null, actor, c, BranchCashMovement.MovementType.INTER_BRANCH_CASH_RECEIVED,
                amt, inv, note);
        auditService.log("INTER_BRANCH_CASH_RECEIVED", "BRANCH_CASH_INVENTORY", inv.getId(), c + " " + amt, actor);
    }

    @Transactional
    public void completePayout(Transaction transaction, User cashier) {
        BranchCashReservation reservation = reservationRepository.findByTransactionIdForUpdate(transaction.getId())
                .orElseThrow(() -> new InvalidTransactionException("No cash reservation exists for transaction " + transaction.getId()));
        if (reservation.getStatus() == BranchCashReservation.ReservationStatus.RELEASED) {
            return;
        }
        if (reservation.getStatus() != BranchCashReservation.ReservationStatus.RESERVED) {
            throw new InvalidTransactionException("Cash reservation is not active for transaction " + transaction.getId());
        }

        BranchCashInventory inventory = inventoryRepository
                .findByBranchIdAndCurrencyForUpdate(reservation.getBranch().getId(), reservation.getCurrency())
                .orElseThrow(() -> new ResourceNotFoundException("Branch cash inventory not found"));
        if (inventory.getReservedBalance().compareTo(reservation.getAmount()) < 0) {
            throw new InvalidTransactionException("Reserved cash balance is inconsistent for transaction " + transaction.getId());
        }

        inventory.setReservedBalance(money(inventory.getReservedBalance().subtract(reservation.getAmount())));
        inventoryRepository.save(inventory);
        reservation.setStatus(BranchCashReservation.ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        recordMovement(reservation.getBranch(), transaction, cashier, reservation.getCurrency(),
                BranchCashMovement.MovementType.TRANSFER_PAYOUT_RELEASED, reservation.getAmount(), inventory,
                "Cash paid to receiver");
        cashierShiftService.recordCashOut(cashier, reservation.getBranch(), reservation.getCurrency(), reservation.getAmount(),
                "TRANSFER", transaction.getId(), "Transfer payout released");
        auditService.log("BRANCH_CASH_PAYOUT_RELEASED", "Transaction", transaction.getId(),
                reservation.getCurrency() + " " + reservation.getAmount(), cashier);
    }

    @Transactional
    public void cancelReservation(Transaction transaction, User actor) {
        BranchCashReservation reservation = reservationRepository.findByTransactionIdForUpdate(transaction.getId())
                .orElse(null);
        if (reservation == null || reservation.getStatus() != BranchCashReservation.ReservationStatus.RESERVED) {
            return;
        }
        BranchCashInventory inventory = inventoryRepository
                .findByBranchIdAndCurrencyForUpdate(reservation.getBranch().getId(), reservation.getCurrency())
                .orElseThrow(() -> new ResourceNotFoundException("Branch cash inventory not found"));
        inventory.setReservedBalance(money(inventory.getReservedBalance().subtract(reservation.getAmount())));
        inventory.setAvailableBalance(money(inventory.getAvailableBalance().add(reservation.getAmount())));
        inventoryRepository.save(inventory);
        reservation.setStatus(BranchCashReservation.ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        recordMovement(reservation.getBranch(), transaction, actor, reservation.getCurrency(),
                BranchCashMovement.MovementType.RESERVATION_CANCELLED, reservation.getAmount(), inventory,
                "Cash reservation cancelled");
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TransferCancelledEvent(
                transaction.getId(),
                transaction.getSender().getId(),
                transaction.getReceiver().getId(),
                "RESERVATION_CANCELLED"));
    }

    private void maybePublishLiquidityLow(Branch branch, String currency, BranchCashInventory inventory) {
        if (inventory.getLowCashThreshold() == null || inventory.getLowCashThreshold().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (inventory.getAvailableBalance().compareTo(inventory.getLowCashThreshold()) < 0) {
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.LiquidityLowEvent(
                    branch.getId(),
                    branch.getName(),
                    currency,
                    inventory.getAvailableBalance(),
                    inventory.getLowCashThreshold()));
        }
    }

    private BranchCashReservation createReservation(Transaction transaction, Branch payoutBranch,
                                                    String currency, BigDecimal amount, User actor) {
        BranchCashInventory inventory = inventoryRepository
                .findByBranchIdAndCurrencyForUpdate(payoutBranch.getId(), currency)
                .orElseThrow(() -> new InsufficientFundsException("No cash inventory for branch "
                        + payoutBranch.getName() + " in " + currency));
        if (inventory.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient cash at branch " + payoutBranch.getName()
                    + " for " + currency + " payout. Required: " + amount
                    + ", Available: " + inventory.getAvailableBalance());
        }

        inventory.setAvailableBalance(money(inventory.getAvailableBalance().subtract(amount)));
        inventory.setReservedBalance(money(inventory.getReservedBalance().add(amount)));
        inventoryRepository.save(inventory);
        maybePublishLiquidityLow(payoutBranch, currency, inventory);

        BranchCashReservation reservation = BranchCashReservation.builder()
                .branch(payoutBranch)
                .transaction(transaction)
                .currency(currency)
                .amount(amount)
                .status(BranchCashReservation.ReservationStatus.RESERVED)
                .build();
        BranchCashReservation saved = reservationRepository.save(reservation);
        recordMovement(payoutBranch, transaction, actor, currency,
                BranchCashMovement.MovementType.TRANSFER_PAYOUT_RESERVED, amount, inventory,
                "Reserved receiver payout cash");
        auditService.log("BRANCH_CASH_PAYOUT_RESERVED", "Transaction", transaction.getId(),
                currency + " " + amount + " at branch " + payoutBranch.getId(), actor);
        return saved;
    }

    private void recordMovement(Branch branch, Transaction transaction, User cashier, String currency,
                                BranchCashMovement.MovementType movementType, BigDecimal amount,
                                BranchCashInventory inventory, String note) {
        movementRepository.save(BranchCashMovement.builder()
                .branch(branch)
                .transaction(transaction)
                .cashier(cashier)
                .currency(currency)
                .movementType(movementType)
                .amount(money(amount))
                .availableAfter(inventory.getAvailableBalance())
                .reservedAfter(inventory.getReservedBalance())
                .note(note)
                .build());
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        return currency.trim().toUpperCase();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4) : value.setScale(4, RoundingMode.HALF_UP);
    }
}
