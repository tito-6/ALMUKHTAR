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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanDisbursementService {

    private final LoanApplicationRepository applicationRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final PlatformRevenueService platformRevenueService;
    private final AuditService auditService;

    public LoanDisbursementService(LoanApplicationRepository applicationRepository,
                                  LoanRepository loanRepository,
                                  LoanRepaymentScheduleRepository scheduleRepository,
                                  WalletService walletService,
                                  WalletRepository walletRepository,
                                  PlatformRevenueService platformRevenueService,
                                  AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.platformRevenueService = platformRevenueService;
        this.auditService = auditService;
    }

    @Transactional
    public Loan disburse(Long applicationId, User approvedBy) {
        LoanApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found: " + applicationId));
        if (!"APPROVED".equals(app.getStatus())) {
            throw new IllegalStateException("Application must be APPROVED to disburse");
        }
        Wallet wallet = walletRepository.findByUser_Id(app.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user"));
        BigDecimal principal = app.getRequestedAmount();
        int termDays = app.getRequestedTermDays();
        BigDecimal apr = app.getProduct().getAprRate();
        BigDecimal originationFeePct = app.getProduct().getOriginationFeePct() != null ? app.getProduct().getOriginationFeePct() : BigDecimal.ZERO;
        BigDecimal originationFee = principal.multiply(originationFeePct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        String currency = "USD";

        int termMonths = (termDays + 29) / 30;
        if (termMonths < 1) termMonths = 1;
        BigDecimal monthlyRate = apr.divide(BigDecimal.valueOf(100 * 12), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowN = onePlusR.pow(termMonths);
        BigDecimal monthlyPayment = principal
                .multiply(monthlyRate)
                .multiply(onePlusRPowN)
                .divide(onePlusRPowN.subtract(BigDecimal.ONE), 4, RoundingMode.HALF_UP);

        BigDecimal netDisburse = principal.subtract(originationFee);
        walletService.credit(wallet.getId(), currency, netDisburse,
                WalletTransactionType.LOAN_DISBURSEMENT, "LOAN-APP-" + applicationId, "Loan disbursement");

        if (originationFee.compareTo(BigDecimal.ZERO) > 0) {
            platformRevenueService.collect("LOAN_ORIGINATION_FEE", originationFee, currency, applicationId, "LOAN_APPLICATION");
        }

        Loan loan = Loan.builder()
                .application(app)
                .user(app.getUser())
                .principalAmount(principal)
                .currency(currency)
                .disbursedAt(Instant.now())
                .termDays(termDays)
                .aprRate(apr)
                .originationFee(originationFee)
                .monthlyPayment(monthlyPayment)
                .outstandingBalance(principal)
                .status("ACTIVE")
                .build();
        loan = loanRepository.save(loan);

        List<LoanRepaymentSchedule> schedule = buildSchedule(loan, termMonths, monthlyRate, monthlyPayment, principal);
        scheduleRepository.saveAll(schedule);

        app.setStatus("DISBURSED");
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);

        auditService.log("LOAN_DISBURSED", "LOAN", loan.getId(),
                "principal=" + principal + " monthlyPayment=" + monthlyPayment, approvedBy);
        return loan;
    }

    private List<LoanRepaymentSchedule> buildSchedule(Loan loan, int termMonths, BigDecimal monthlyRate,
                                                       BigDecimal monthlyPayment, BigDecimal principal) {
        List<LoanRepaymentSchedule> list = new ArrayList<>();
        LocalDate firstDue = LocalDate.now(ZoneId.systemDefault()).plusMonths(1);
        BigDecimal remaining = principal;
        for (int i = 1; i <= termMonths; i++) {
            BigDecimal interestDue = remaining.multiply(monthlyRate).setScale(4, RoundingMode.HALF_UP);
            BigDecimal principalDue = monthlyPayment.subtract(interestDue);
            if (i == termMonths) {
                principalDue = remaining;
            }
            if (principalDue.compareTo(remaining) > 0) principalDue = remaining;
            BigDecimal totalDue = principalDue.add(interestDue);
            remaining = remaining.subtract(principalDue);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

            LoanRepaymentSchedule row = LoanRepaymentSchedule.builder()
                    .loan(loan)
                    .instalmentNumber(i)
                    .dueDate(firstDue.plusMonths(i - 1))
                    .principalDue(principalDue)
                    .interestDue(interestDue)
                    .totalDue(totalDue)
                    .status("PENDING")
                    .build();
            list.add(row);
        }
        return list;
    }
}
