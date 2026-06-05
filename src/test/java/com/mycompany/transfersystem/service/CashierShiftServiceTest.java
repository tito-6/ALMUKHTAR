package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.cashier.CloseShiftRequest;
import com.mycompany.transfersystem.dto.cashier.OpenShiftRequest;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.exception.InvalidTransactionException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.cashier.CashierShiftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashierShiftServiceTest {

    @Mock private BranchRepository branchRepository;
    @Mock private CashierShiftRepository shiftRepository;
    @Mock private CashierShiftBalanceRepository balanceRepository;
    @Mock private CashierShiftEntryRepository entryRepository;
    @Mock private AuditService auditService;

    @InjectMocks private CashierShiftService service;

    private Branch branch;
    private User cashier;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setId(2L);
        branch.setName("BRANCH_A");

        cashier = new User();
        cashier.setId(10L);
        cashier.setUsername("cashier");
    }

    @Test
    void openShift_createsOpeningBalances() {
        OpenShiftRequest request = new OpenShiftRequest();
        request.setBranchId(2L);
        request.setOpeningBalances(Map.of("usd", new BigDecimal("100.00")));
        when(shiftRepository.findByCashierIdAndStatus(10L, CashierShift.ShiftStatus.OPEN)).thenReturn(Optional.empty());
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branch));
        when(shiftRepository.save(any())).thenAnswer(invocation -> {
            CashierShift shift = invocation.getArgument(0);
            shift.setId(5L);
            return shift;
        });

        CashierShift shift = service.openShift(request, cashier);

        assertThat(shift.getStatus()).isEqualTo(CashierShift.ShiftStatus.OPEN);
        verify(balanceRepository).save(argThat(balance ->
                balance.getCurrency().equals("USD")
                        && balance.getOpeningBalance().compareTo(new BigDecimal("100.0000")) == 0
                        && balance.getExpectedClosingBalance().compareTo(new BigDecimal("100.0000")) == 0));
    }

    @Test
    void openShift_rejectsSecondOpenShiftForSameCashier() {
        CashierShift existing = CashierShift.builder().id(4L).status(CashierShift.ShiftStatus.OPEN).build();
        when(shiftRepository.findByCashierIdAndStatus(10L, CashierShift.ShiftStatus.OPEN)).thenReturn(Optional.of(existing));

        OpenShiftRequest request = new OpenShiftRequest();
        request.setBranchId(2L);

        assertThatThrownBy(() -> service.openShift(request, cashier))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("already has an open shift");
    }

    @Test
    void recordAndCloseShift_calculatesExpectedAndVariance() {
        CashierShift shift = CashierShift.builder()
                .id(8L)
                .branch(branch)
                .cashier(cashier)
                .status(CashierShift.ShiftStatus.OPEN)
                .build();
        CashierShiftBalance balance = CashierShiftBalance.builder()
                .shift(shift)
                .currency("USD")
                .openingBalance(new BigDecimal("100.0000"))
                .cashInTotal(BigDecimal.ZERO.setScale(4))
                .cashOutTotal(BigDecimal.ZERO.setScale(4))
                .expectedClosingBalance(new BigDecimal("100.0000"))
                .build();
        when(shiftRepository.findOpenByCashierIdForUpdate(10L)).thenReturn(Optional.of(shift));
        when(balanceRepository.findByShiftIdAndCurrencyForUpdate(8L, "USD")).thenReturn(Optional.of(balance));

        service.recordCashIn(cashier, branch, "USD", new BigDecimal("50.00"), "TOPUP", 1L, "topup");
        service.recordCashOut(cashier, branch, "USD", new BigDecimal("30.00"), "TRANSFER", 2L, "payout");

        assertThat(balance.getCashInTotal()).isEqualByComparingTo("50.0000");
        assertThat(balance.getCashOutTotal()).isEqualByComparingTo("30.0000");
        assertThat(balance.getExpectedClosingBalance()).isEqualByComparingTo("120.0000");

        CloseShiftRequest close = new CloseShiftRequest();
        close.setShiftId(8L);
        close.setCountedBalances(Map.of("USD", new BigDecimal("119.50")));
        when(shiftRepository.findById(8L)).thenReturn(Optional.of(shift));
        when(balanceRepository.findByShiftId(8L)).thenReturn(List.of(balance));
        when(shiftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CashierShift closed = service.closeShift(close, cashier);

        assertThat(closed.getStatus()).isEqualTo(CashierShift.ShiftStatus.CLOSED);
        assertThat(balance.getActualClosingBalance()).isEqualByComparingTo("119.5000");
        assertThat(balance.getVariance()).isEqualByComparingTo("-0.5000");
        verify(entryRepository, times(2)).save(any(CashierShiftEntry.class));
    }
}
