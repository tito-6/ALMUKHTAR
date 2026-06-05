package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.WalletBalance;
import com.mycompany.transfersystem.entity.WalletTransaction;
import com.mycompany.transfersystem.entity.enums.WalletStatus;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.exception.InsufficientWalletBalanceException;
import com.mycompany.transfersystem.exception.WalletFrozenException;
import com.mycompany.transfersystem.exception.WalletNotFoundException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.wallet.WalletService;
import com.mycompany.transfersystem.service.CurrencyConversionService;
import com.mycompany.transfersystem.service.accounting.AccountingLedgerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletBalanceRepository walletBalanceRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private CurrencyConversionService currencyConversionService;
    @Mock private CommissionRateRepository commissionRateRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private AccountingLedgerService accountingLedgerService;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private WalletService walletService;

    @Test
    void credit_updatesBalance_createsLedgerEntry_firesEvent() {
        Wallet wallet = Wallet.builder().id(1L).status(WalletStatus.ACTIVE).build();
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        WalletBalance balance = WalletBalance.builder().wallet(wallet).currencyCode("USD")
                .availableBalance(new BigDecimal("100")).lockedBalance(BigDecimal.ZERO).build();
        when(walletBalanceRepository.findByWalletIdAndCurrencyCodeForUpdate(1L, "USD")).thenReturn(Optional.of(balance));
        when(walletBalanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any())).thenAnswer(i -> {
            WalletTransaction tx = (WalletTransaction) i.getArgument(0);
            tx.setReferenceId("CR-123");
            return tx;
        });

        WalletTransaction result = walletService.credit(1L, "USD", new BigDecimal("50"),
                WalletTransactionType.TOPUP, "REF-1", "Test credit");

        assertNotNull(result);
        assertEquals(new BigDecimal("150"), balance.getAvailableBalance());
        verify(eventPublisher).publishEvent(any(com.mycompany.transfersystem.event.WalletCreditedEvent.class));
    }

    @Test
    void debit_insufficientBalance_throwsException() {
        Wallet wallet = Wallet.builder().id(1L).status(WalletStatus.ACTIVE).build();
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        WalletBalance balance = WalletBalance.builder().wallet(wallet).currencyCode("USD")
                .availableBalance(new BigDecimal("100")).build();
        when(walletBalanceRepository.findByWalletIdAndCurrencyCodeForUpdate(1L, "USD")).thenReturn(Optional.of(balance));

        assertThrows(InsufficientWalletBalanceException.class, () ->
                walletService.debit(1L, "USD", new BigDecimal("200"),
                        WalletTransactionType.WITHDRAWAL, "REF-2", "Test debit"));
    }

    @Test
    void freeze_frozenWallet_rejectsFutureDebits() {
        Wallet wallet = Wallet.builder().id(1L).status(WalletStatus.FROZEN).build();
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(wallet));

        assertThrows(WalletFrozenException.class, () ->
                walletService.debit(1L, "USD", new BigDecimal("10"),
                        WalletTransactionType.WITHDRAWAL, "REF-3", "Frozen test"));
    }

    @Test
    void debit_walletNotFound_throwsException() {
        when(walletRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () ->
                walletService.debit(99L, "USD", new BigDecimal("10"),
                        WalletTransactionType.WITHDRAWAL, "REF-4", "Not found test"));
    }
}
