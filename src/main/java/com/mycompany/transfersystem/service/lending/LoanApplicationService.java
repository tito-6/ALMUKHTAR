package com.mycompany.transfersystem.service.lending;

import com.mycompany.transfersystem.entity.LoanApplication;
import com.mycompany.transfersystem.entity.LoanProduct;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.LoanApplicationRepository;
import com.mycompany.transfersystem.repository.LoanProductRepository;
import com.mycompany.transfersystem.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final LoanProductRepository productRepository;
    private final CreditScoringService creditScoringService;
    private final AuditService auditService;

    public LoanApplicationService(LoanApplicationRepository applicationRepository,
                                  LoanProductRepository productRepository,
                                  CreditScoringService creditScoringService,
                                  AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.productRepository = productRepository;
        this.creditScoringService = creditScoringService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public boolean canApply(Long userId) {
        return creditScoringService.isEligibleForLoan(userId);
    }

    @Transactional
    public LoanApplication apply(Long userId, LoanProduct product, BigDecimal requestedAmount,
                                  int requestedTermDays, String purpose, User applicant) {
        if (!creditScoringService.isEligibleForLoan(userId)) {
            throw new IllegalStateException("User not eligible for loans (trust score or credit score too low)");
        }
        if (requestedAmount.compareTo(product.getMinAmount()) < 0 || requestedAmount.compareTo(product.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("Requested amount outside product limits");
        }
        if (requestedTermDays < product.getMinTermDays() || requestedTermDays > product.getMaxTermDays()) {
            throw new IllegalArgumentException("Requested term outside product limits");
        }
        com.mycompany.transfersystem.entity.CreditProfile profile = creditScoringService.calculateAndSave(userId);
        if (profile.getMaxLoanAmountUsd().compareTo(requestedAmount) < 0) {
            throw new IllegalStateException("Requested amount exceeds your max loan amount: " + profile.getMaxLoanAmountUsd());
        }
        LoanApplication app = LoanApplication.builder()
                .user(applicant)
                .product(product)
                .requestedAmount(requestedAmount)
                .requestedTermDays(requestedTermDays)
                .purpose(purpose)
                .status("PENDING")
                .build();
        app = applicationRepository.save(app);
        auditService.log("LOAN_APPLICATION_SUBMITTED", "LOAN_APPLICATION", app.getId(),
                "amount=" + requestedAmount + " termDays=" + requestedTermDays, applicant);
        return app;
    }

    @Transactional(readOnly = true)
    public List<LoanApplication> getPendingApplications() {
        return applicationRepository.findByStatusOrderByCreatedAtAsc("PENDING");
    }

    @Transactional(readOnly = true)
    public List<LoanApplication> getMyApplications(Long userId) {
        return applicationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public LoanApplication getById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan application not found: " + id));
    }

    @Transactional
    public LoanApplication approve(Long applicationId, User reviewer) {
        LoanApplication app = getById(applicationId);
        if (!"PENDING".equals(app.getStatus())) {
            throw new IllegalStateException("Application is not PENDING");
        }
        app.setStatus("APPROVED");
        app.setReviewedBy(reviewer);
        app.setDecisionReason("Approved");
        app.setUpdatedAt(Instant.now());
        app = applicationRepository.save(app);
        auditService.log("LOAN_APPROVED", "LOAN_APPLICATION", app.getId(), "Approved by admin", reviewer);
        return app;
    }

    @Transactional
    public LoanApplication reject(Long applicationId, String reason, User reviewer) {
        LoanApplication app = getById(applicationId);
        if (!"PENDING".equals(app.getStatus())) {
            throw new IllegalStateException("Application is not PENDING");
        }
        app.setStatus("REJECTED");
        app.setReviewedBy(reviewer);
        app.setDecisionReason(reason != null ? reason : "Rejected");
        app.setUpdatedAt(Instant.now());
        app = applicationRepository.save(app);
        auditService.log("LOAN_REJECTED", "LOAN_APPLICATION", app.getId(), reason != null ? reason : "", reviewer);
        return app;
    }
}
