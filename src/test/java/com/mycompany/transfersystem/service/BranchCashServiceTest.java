package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.exception.InsufficientFundsException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.cashier.CashierShiftService;
import com.mycompany.transfersystem.service.liquidity.BranchCashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchCashServiceTest {

    @Mock private BranchRepository branchRepository;
    @Mock private BranchCashInventoryRepository inventoryRepository;
    @Mock private BranchCashReservationRepository reservationRepository;
    @Mock private BranchCashMovementRepository movementRepository;
    @Mock private AuditService auditService;
    @Mock private CashierShiftService cashierShiftService;

    @InjectMocks private BranchCashService branchCashService;

    private Branch branch;
    private Transaction transaction;
    private User actor;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setId(2L);
        branch.setName("BRANCH_B");

        transaction = new Transaction();
        transaction.setId(99L);

        actor = new User();
        actor.setId(7L);
        actor.setUsername("cashier");
    }

    @Test
    void reservePayout_movesCashFromAvailableToReserved() {
        BranchCashInventory inventory = inventory("USD", "500.0000", "0.0000");
        when(reservationRepository.findByTransactionIdForUpdate(99L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByBranchIdAndCurrencyForUpdate(2L, "USD")).thenReturn(Optional.of(inventory));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BranchCashReservation reservation = branchCashService.reservePayout(
                transaction, branch, "usd", new BigDecimal("125.25"), actor);

        assertThat(inventory.getAvailableBalance()).isEqualByComparingTo("374.7500");
        assertThat(inventory.getReservedBalance()).isEqualByComparingTo("125.2500");
        assertThat(reservation.getStatus()).isEqualTo(BranchCashReservation.ReservationStatus.RESERVED);
        assertThat(reservation.getCurrency()).isEqualTo("USD");
        verify(movementRepository).save(any(BranchCashMovement.class));
    }

    @Test
    void reservePayout_rejectsWhenBranchCashIsInsufficient() {
        BranchCashInventory inventory = inventory("USD", "50.0000", "0.0000");
        when(reservationRepository.findByTransactionIdForUpdate(99L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByBranchIdAndCurrencyForUpdate(2L, "USD")).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> branchCashService.reservePayout(
                transaction, branch, "USD", new BigDecimal("100.00"), actor))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient cash");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void completePayout_consumesReservedCashAndMarksReservationReleased() {
        BranchCashInventory inventory = inventory("USD", "375.0000", "125.0000");
        BranchCashReservation reservation = BranchCashReservation.builder()
                .branch(branch)
                .transaction(transaction)
                .currency("USD")
                .amount(new BigDecimal("125.0000"))
                .status(BranchCashReservation.ReservationStatus.RESERVED)
                .build();
        when(reservationRepository.findByTransactionIdForUpdate(99L)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByBranchIdAndCurrencyForUpdate(2L, "USD")).thenReturn(Optional.of(inventory));

        branchCashService.completePayout(transaction, actor);

        assertThat(inventory.getAvailableBalance()).isEqualByComparingTo("375.0000");
        assertThat(inventory.getReservedBalance()).isEqualByComparingTo("0.0000");
        assertThat(reservation.getStatus()).isEqualTo(BranchCashReservation.ReservationStatus.RELEASED);
        verify(reservationRepository).save(reservation);
        verify(movementRepository).save(any(BranchCashMovement.class));
    }

    private BranchCashInventory inventory(String currency, String available, String reserved) {
        return BranchCashInventory.builder()
                .branch(branch)
                .currency(currency)
                .availableBalance(new BigDecimal(available))
                .reservedBalance(new BigDecimal(reserved))
                .lowCashThreshold(BigDecimal.ZERO)
                .highCashThreshold(BigDecimal.ZERO)
                .build();
    }
}
