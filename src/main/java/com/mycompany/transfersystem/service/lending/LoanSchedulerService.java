package com.mycompany.transfersystem.service.lending;

import com.mycompany.transfersystem.entity.Loan;
import com.mycompany.transfersystem.entity.LoanLateFee;
import com.mycompany.transfersystem.entity.LoanRepayment;
import com.mycompany.transfersystem.entity.LoanRepaymentSchedule;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.GamificationService;
import com.mycompany.transfersystem.service.revenue.PlatformRevenueService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class LoanSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(LoanSchedulerService.class);
    private static final int CREDIT_SCORE_MISSED_PENALTY = 40;
    private static final int CREDIT_SCORE_PAID_OFF_BONUS = 50;

    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final LoanLateFeeRepository lateFeeRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CreditScoringService creditScoringService;
    private final GamificationService gamificationService;
    private final PlatformRevenueService platformRevenueService;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public LoanSchedulerService(LoanRepaymentScheduleRepository scheduleRepository,
                                LoanRepository loanRepository,
                                LoanRepaymentRepository repaymentRepository,
                                LoanLateFeeRepository lateFeeRepository,
                                WalletService walletService,
                                WalletRepository walletRepository,
                                WalletTransactionRepository walletTransactionRepository,
                                CreditScoringService creditScoringService,
                                GamificationService gamificationService,
                                PlatformRevenueService platformRevenueService,
                                AuditService auditService,
                                ApplicationEventPublisher applicationEventPublisher) {
        this.scheduleRepository = scheduleRepository;
        this.loanRepository = loanRepository;
        this.repaymentRepository = repaymentRepository;
        this.lateFeeRepository = lateFeeRepository;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.creditScoringService = creditScoringService;
        this.gamificationService = gamificationService;
        this.platformRevenueService = platformRevenueService;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @SchedulerLock(name = "loan-scheduler", lockAtMostFor = "PT30M")
    @Transactional
    public void processDueInstalments() {
        LocalDate today = LocalDate.now();
        List<LoanRepaymentSchedule> dueToday = scheduleRepository.findByDueDateAndStatus(today, "PENDING");
        for (LoanRepaymentSchedule schedule : dueToday) {
            try {
                Loan loan = schedule.getLoan();
                BigDecimal totalDue = schedule.getTotalDue().subtract(schedule.getPaidAmount());
                if (totalDue.compareTo(BigDecimal.ZERO) > 0) {
                    applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.LoanPaymentDueEvent(
                            loan.getId(),
                            loan.getUser().getId(),
                            schedule.getId(),
                            totalDue,
                            loan.getCurrency(),
                            schedule.getDueDate() != null ? schedule.getDueDate().toString() : ""));
                }
                processOneInstalment(schedule);
            } catch (Exception e) {
                log.warn("Failed to process schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }
        markDefaultedLoans();
        markPaidOffLoans();
    }

    private void processOneInstalment(LoanRepaymentSchedule schedule) {
        Loan loan = schedule.getLoan();
        Wallet wallet = walletRepository.findByUser_Id(loan.getUser().getId()).orElse(null);
        if (wallet == null) {
            markMissed(schedule);
            return;
        }
        BigDecimal totalDue = schedule.getTotalDue().subtract(schedule.getPaidAmount());
        if (totalDue.compareTo(BigDecimal.ZERO) <= 0) {
            schedule.setStatus("PAID");
            scheduleRepository.save(schedule);
            return;
        }
        com.mycompany.transfersystem.entity.WalletTransaction wtx;
        try {
            wtx = walletService.debit(wallet.getId(), loan.getCurrency(), totalDue,
                    WalletTransactionType.LOAN_REPAYMENT, "LOAN-AUTO-" + loan.getId() + "-" + schedule.getInstalmentNumber(), "Auto loan instalment");
        } catch (Exception e) {
            markMissed(schedule);
            return;
        }
        if (schedule.getInterestDue().compareTo(BigDecimal.ZERO) > 0) {
            platformRevenueService.collect("LOAN_INTEREST", schedule.getInterestDue(), loan.getCurrency(), loan.getId(), "LOAN");
        }
        schedule.setPaidAmount(schedule.getTotalDue());
        schedule.setPaidAt(Instant.now());
        schedule.setStatus("PAID");
        scheduleRepository.save(schedule);
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(totalDue));
        loanRepository.save(loan);
        LoanRepayment rep = LoanRepayment.builder()
                .loan(loan)
                .schedule(schedule)
                .amount(totalDue)
                .paymentMethod("WALLET_AUTO")
                .walletTransaction(wtx)
                .build();
        repaymentRepository.save(rep);
        auditService.log("LOAN_REPAYMENT_SUCCESS", "LOAN", loan.getId(), "Auto debit amount=" + totalDue, loan.getUser());
    }

    private void markMissed(LoanRepaymentSchedule schedule) {
        schedule.setStatus("MISSED");
        scheduleRepository.save(schedule);
        Loan loan = schedule.getLoan();
        BigDecimal lateFee = loan.getApplication().getProduct().getLateFeeAmount();
        if (lateFee == null) lateFee = BigDecimal.ZERO;
        if (lateFee.compareTo(BigDecimal.ZERO) > 0) {
            LoanLateFee lf = LoanLateFee.builder()
                    .loan(loan)
                    .schedule(schedule)
                    .feeAmount(lateFee)
                    .collected(false)
                    .build();
            lateFeeRepository.save(lf);
            platformRevenueService.collect("LOAN_LATE_FEE", lateFee, loan.getCurrency(), loan.getId(), "LOAN");
        }
        creditScoringService.reduceScoreByPoints(loan.getUser().getId(), CREDIT_SCORE_MISSED_PENALTY);
        auditService.log("LOAN_REPAYMENT_MISSED", "LOAN", loan.getId(), "Schedule " + schedule.getInstalmentNumber(), loan.getUser());
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.LoanPaymentFailedEvent(
                loan.getId(), loan.getUser().getId(), schedule.getId(), "AUTO_DEBIT_FAILED", lateFee, false));
    }

    private void markDefaultedLoans() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Loan> loansToDefault = scheduleRepository.findLoansWithMissedScheduleBefore(thirtyDaysAgo);
        for (Loan loan : loansToDefault) {
            if (!"ACTIVE".equals(loan.getStatus())) continue;
            loan.setStatus("DEFAULTED");
            loanRepository.save(loan);
            walletRepository.findByUser_Id(loan.getUser().getId()).ifPresent(w -> walletService.freezeWallet(w.getId(),
                    "LOAN_DEFAULT", "FRZ-LOAN-" + loan.getId(),
                    "Your wallet is restricted due to loan default escalation.",
                    "/api/disputes"));
            auditService.log("LOAN_DEFAULTED", "LOAN", loan.getId(), "Escalated after 30+ days missed", loan.getUser());
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.LoanPaymentFailedEvent(
                    loan.getId(), loan.getUser().getId(), 0L, "DEFAULT_ESCALATION", BigDecimal.ZERO, true));
        }
    }

    private void markPaidOffLoans() {
        for (Loan loan : loanRepository.findByStatus("ACTIVE")) {
            if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
                loan.setStatus("PAID_OFF");
                loanRepository.save(loan);
                creditScoringService.addScorePoints(loan.getUser().getId(), CREDIT_SCORE_PAID_OFF_BONUS);
                gamificationService.awardBadge(loan.getUser().getId(), "DEBT_FREE");
                auditService.log("LOAN_PAID_OFF", "LOAN", loan.getId(), "Full repayment", loan.getUser());
            }
        }
    }
}
