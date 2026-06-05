package com.mycompany.transfersystem.service.cashier;

import com.mycompany.transfersystem.dto.cashier.CloseShiftRequest;
import com.mycompany.transfersystem.dto.cashier.OpenShiftRequest;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.exception.InvalidTransactionException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CashierShiftService {

    private final BranchRepository branchRepository;
    private final CashierShiftRepository shiftRepository;
    private final CashierShiftBalanceRepository balanceRepository;
    private final CashierShiftEntryRepository entryRepository;
    private final AuditService auditService;

    @Transactional
    public CashierShift openShift(OpenShiftRequest request, User cashier) {
        shiftRepository.findByCashierIdAndStatus(cashier.getId(), CashierShift.ShiftStatus.OPEN)
                .ifPresent(existing -> {
                    throw new InvalidTransactionException("Cashier already has an open shift: " + existing.getId());
                });
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));

        CashierShift shift = shiftRepository.save(CashierShift.builder()
                .branch(branch)
                .cashier(cashier)
                .status(CashierShift.ShiftStatus.OPEN)
                .notes(request.getNotes())
                .build());

        if (request.getOpeningBalances() != null) {
            request.getOpeningBalances().forEach((currency, amount) ->
                    balanceRepository.save(CashierShiftBalance.builder()
                            .shift(shift)
                            .currency(normalizeCurrency(currency))
                            .openingBalance(money(amount))
                            .expectedClosingBalance(money(amount))
                            .build()));
        }

        auditService.log("CASHIER_SHIFT_OPENED", "CASHIER_SHIFT", shift.getId(),
                "branch=" + branch.getId(), cashier);
        return shift;
    }

    @Transactional
    public CashierShift closeShift(CloseShiftRequest request, User cashier) {
        CashierShift shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + request.getShiftId()));
        if (!shift.getCashier().getId().equals(cashier.getId())) {
            throw new InvalidTransactionException("Only the assigned cashier can close this shift");
        }
        if (shift.getStatus() != CashierShift.ShiftStatus.OPEN) {
            throw new InvalidTransactionException("Shift is not open");
        }

        Map<String, BigDecimal> counted = request.getCountedBalances() == null ? Map.of() : request.getCountedBalances();
        for (CashierShiftBalance balance : balanceRepository.findByShiftId(shift.getId())) {
            BigDecimal expected = balance.getOpeningBalance()
                    .add(balance.getCashInTotal())
                    .subtract(balance.getCashOutTotal())
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal actual = money(counted.getOrDefault(balance.getCurrency(), expected));
            balance.setExpectedClosingBalance(expected);
            balance.setActualClosingBalance(actual);
            balance.setVariance(actual.subtract(expected).setScale(4, RoundingMode.HALF_UP));
            balanceRepository.save(balance);
        }

        shift.setStatus(CashierShift.ShiftStatus.CLOSED);
        shift.setClosedAt(LocalDateTime.now());
        shift.setNotes(request.getNotes());
        shift = shiftRepository.save(shift);
        auditService.log("CASHIER_SHIFT_CLOSED", "CASHIER_SHIFT", shift.getId(), "closed by cashier", cashier);
        return shift;
    }

    @Transactional
    public CashierShift approveShift(Long shiftId, User approver) {
        CashierShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + shiftId));
        if (shift.getStatus() != CashierShift.ShiftStatus.CLOSED) {
            throw new InvalidTransactionException("Only closed shifts can be approved");
        }
        shift.setStatus(CashierShift.ShiftStatus.APPROVED);
        shift.setApprovedBy(approver);
        shift.setApprovedAt(LocalDateTime.now());
        shift = shiftRepository.save(shift);
        auditService.log("CASHIER_SHIFT_APPROVED", "CASHIER_SHIFT", shiftId, null, approver);
        return shift;
    }

    @Transactional(readOnly = true)
    public List<CashierShift> getOpenShifts(Long branchId) {
        return shiftRepository.findByBranchIdAndStatus(branchId, CashierShift.ShiftStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public List<CashierShiftBalance> getShiftBalances(Long shiftId) {
        return balanceRepository.findByShiftId(shiftId);
    }

    @Transactional(readOnly = true)
    public List<CashierShiftEntry> getShiftEntries(Long shiftId) {
        return entryRepository.findByShiftIdOrderByCreatedAtAsc(shiftId);
    }

    @Transactional
    public void recordCashIn(User cashier, Branch branch, String currency, BigDecimal amount,
                             String referenceType, Long referenceId, String note) {
        recordEntry(cashier, branch, currency, amount, CashierShiftEntry.EntryType.CASH_IN, referenceType, referenceId, note);
    }

    @Transactional
    public void recordCashOut(User cashier, Branch branch, String currency, BigDecimal amount,
                              String referenceType, Long referenceId, String note) {
        recordEntry(cashier, branch, currency, amount, CashierShiftEntry.EntryType.CASH_OUT, referenceType, referenceId, note);
    }

    private void recordEntry(User cashier, Branch branch, String currency, BigDecimal amount,
                             CashierShiftEntry.EntryType entryType, String referenceType, Long referenceId, String note) {
        if (cashier == null || cashier.getId() == null) {
            return;
        }
        CashierShift shift = shiftRepository.findOpenByCashierIdForUpdate(cashier.getId()).orElse(null);
        if (shift == null) {
            return;
        }
        if (!shift.getBranch().getId().equals(branch.getId())) {
            throw new InvalidTransactionException("Cashier open shift belongs to another branch");
        }
        String normalizedCurrency = normalizeCurrency(currency);
        CashierShiftBalance balance = balanceRepository.findByShiftIdAndCurrencyForUpdate(shift.getId(), normalizedCurrency)
                .orElseGet(() -> balanceRepository.save(CashierShiftBalance.builder()
                        .shift(shift)
                        .currency(normalizedCurrency)
                        .openingBalance(BigDecimal.ZERO.setScale(4))
                        .expectedClosingBalance(BigDecimal.ZERO.setScale(4))
                        .build()));
        BigDecimal normalizedAmount = money(amount);
        if (entryType == CashierShiftEntry.EntryType.CASH_IN) {
            balance.setCashInTotal(balance.getCashInTotal().add(normalizedAmount));
        } else if (entryType == CashierShiftEntry.EntryType.CASH_OUT) {
            balance.setCashOutTotal(balance.getCashOutTotal().add(normalizedAmount));
        }
        balance.setExpectedClosingBalance(balance.getOpeningBalance()
                .add(balance.getCashInTotal())
                .subtract(balance.getCashOutTotal())
                .setScale(4, RoundingMode.HALF_UP));
        balanceRepository.save(balance);

        entryRepository.save(CashierShiftEntry.builder()
                .shift(shift)
                .currency(normalizedCurrency)
                .entryType(entryType)
                .amount(normalizedAmount)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .note(note)
                .build());
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4) : value.setScale(4, RoundingMode.HALF_UP);
    }
}
