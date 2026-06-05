package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.lending.CreditScoringService;
import com.mycompany.transfersystem.service.lending.LoanSchedulerService;
import com.mycompany.transfersystem.service.revenue.PlatformRevenueService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanSchedulerServiceTest {

    @Mock private LoanRepaymentScheduleRepository scheduleRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private LoanRepaymentRepository repaymentRepository;
    @Mock private LoanLateFeeRepository lateFeeRepository;
    @Mock private WalletService walletService;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private CreditScoringService creditScoringService;
    @Mock private GamificationService gamificationService;
    @Mock private PlatformRevenueService platformRevenueService;
    @Mock private AuditService auditService;
    @Mock private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks private LoanSchedulerService loanSchedulerService;

    @Test
    void processDue_walletSufficient_marksPaid() {
        User user = new User(); user.setId(1L);
        LoanProduct product = LoanProduct.builder().lateFeeAmount(new BigDecimal("25")).build();
        LoanApplication app = LoanApplication.builder().product(product).build();
        Loan loan = Loan.builder().id(1L).user(user).currency("USD").status("ACTIVE")
                .outstandingBalance(new BigDecimal("500")).application(app).build();
        LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                .id(1L).loan(loan).totalDue(new BigDecimal("100")).paidAmount(BigDecimal.ZERO)
                .interestDue(new BigDecimal("10")).instalmentNumber(1).status("PENDING")
                .dueDate(LocalDate.now())
                .build();

        when(scheduleRepository.findByDueDateAndStatus(any(LocalDate.class), eq("PENDING"))).thenReturn(List.of(schedule));
        Wallet wallet = Wallet.builder().id(10L).build();
        when(walletRepository.findByUser_Id(1L)).thenReturn(Optional.of(wallet));
        WalletTransaction wtx = WalletTransaction.builder().build();
        when(walletService.debit(anyLong(), anyString(), any(), any(), anyString(), anyString())).thenReturn(wtx);
        when(scheduleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(scheduleRepository.findLoansWithMissedScheduleBefore(any())).thenReturn(List.of());
        when(loanRepository.findByStatus("ACTIVE")).thenReturn(List.of());

        loanSchedulerService.processDueInstalments();

        verify(walletService).debit(eq(10L), eq("USD"), eq(new BigDecimal("100")), any(), anyString(), anyString());
        verify(scheduleRepository).save(argThat(s -> "PAID".equals(s.getStatus())));
    }

    @Test
    void processDue_walletInsufficient_marksMissed() {
        User user = new User(); user.setId(1L);
        LoanProduct product = LoanProduct.builder().lateFeeAmount(new BigDecimal("25")).build();
        LoanApplication app = LoanApplication.builder().product(product).build();
        Loan loan = Loan.builder().id(1L).user(user).currency("USD").status("ACTIVE")
                .outstandingBalance(new BigDecimal("500")).application(app).build();
        LoanRepaymentSchedule schedule = LoanRepaymentSchedule.builder()
                .id(1L).loan(loan).totalDue(new BigDecimal("100")).paidAmount(BigDecimal.ZERO)
                .interestDue(BigDecimal.ZERO).instalmentNumber(1).status("PENDING")
                .dueDate(LocalDate.now())
                .build();

        when(scheduleRepository.findByDueDateAndStatus(any(LocalDate.class), eq("PENDING"))).thenReturn(List.of(schedule));
        when(walletRepository.findByUser_Id(1L)).thenReturn(Optional.of(Wallet.builder().id(10L).build()));
        when(walletService.debit(anyLong(), anyString(), any(), any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Insufficient balance"));
        when(scheduleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(lateFeeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(scheduleRepository.findLoansWithMissedScheduleBefore(any())).thenReturn(List.of());
        when(loanRepository.findByStatus("ACTIVE")).thenReturn(List.of());

        loanSchedulerService.processDueInstalments();

        verify(scheduleRepository).save(argThat(s -> "MISSED".equals(s.getStatus())));
    }
}
