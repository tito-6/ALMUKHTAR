package com.mycompany.transfersystem.service.lending;

import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.revenue.PlatformRevenueService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class LoanRepaymentService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PlatformRevenueService platformRevenueService;
    private final AuditService auditService;

    public LoanRepaymentService(LoanRepository loanRepository,
                                LoanRepaymentScheduleRepository scheduleRepository,
                                LoanRepaymentRepository repaymentRepository,
                                WalletService walletService,
                                WalletRepository walletRepository,
                                WalletTransactionRepository walletTransactionRepository,
                                PlatformRevenueService platformRevenueService,
                                AuditService auditService) {
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.repaymentRepository = repaymentRepository;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.platformRevenueService = platformRevenueService;
        this.auditService = auditService;
    }

    @Transactional
    public WalletTransaction repay(Long loanId, BigDecimal amount, Long scheduleId, User payer, String paymentMethod) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        if (!"ACTIVE".equals(loan.getStatus())) {
            throw new IllegalStateException("Loan is not active for repayment");
        }
        if (!loan.getUser().getId().equals(payer.getId())) {
            throw new SecurityException("Not your loan");
        }
        Wallet wallet = walletRepository.findByUser_Id(payer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        List<LoanRepaymentSchedule> pending = scheduleRepository.findByLoan_IdAndStatus(loanId, "PENDING");
        if (pending.isEmpty()) {
            throw new IllegalStateException("No pending instalments");
        }
        LoanRepaymentSchedule schedule = scheduleId != null
                ? pending.stream().filter(s -> s.getId().equals(scheduleId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found or not pending"))
                : pending.get(0);
        BigDecimal toPay = amount.min(schedule.getTotalDue().subtract(schedule.getPaidAmount()));
        if (toPay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Nothing due on this instalment");
        }
        WalletTransaction tx = walletService.debit(wallet.getId(), loan.getCurrency(), toPay,
                WalletTransactionType.LOAN_REPAYMENT, "LOAN-" + loanId + "-SCH-" + schedule.getId(), "Loan repayment");
        BigDecimal interestPortion = schedule.getTotalDue().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : toPay.multiply(schedule.getInterestDue()).divide(schedule.getTotalDue(), 4, RoundingMode.HALF_UP);
        if (interestPortion.compareTo(BigDecimal.ZERO) > 0) {
            platformRevenueService.collect("LOAN_INTEREST", interestPortion, loan.getCurrency(), loan.getId(), "LOAN");
        }
        schedule.setPaidAmount(schedule.getPaidAmount().add(toPay));
        schedule.setPaidAt(Instant.now());
        schedule.setStatus(schedule.getPaidAmount().compareTo(schedule.getTotalDue()) >= 0 ? "PAID" : "PARTIAL");
        scheduleRepository.save(schedule);
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(toPay));
        loanRepository.save(loan);
        LoanRepayment repayment = LoanRepayment.builder()
                .loan(loan)
                .schedule(schedule)
                .amount(toPay)
                .paymentMethod(paymentMethod != null ? paymentMethod : "WALLET_MANUAL")
                .walletTransaction(tx)
                .build();
        repaymentRepository.save(repayment);
        auditService.log("LOAN_REPAYMENT_SUCCESS", "LOAN", loan.getId(), "amount=" + toPay, payer);
        return tx;
    }
}
