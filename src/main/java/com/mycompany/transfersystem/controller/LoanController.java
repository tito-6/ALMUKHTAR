package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.annotation.RequireIdempotencyKey;
import com.mycompany.transfersystem.dto.lending.*;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.lending.*;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lending")
@CrossOrigin(origins = "*")
public class LoanController {

    private final CreditScoringService creditScoringService;
    private final LoanApplicationService loanApplicationService;
    private final LoanDisbursementService loanDisbursementService;
    private final LoanRepaymentService loanRepaymentService;
    private final LoanRiskService loanRiskService;
    private final com.mycompany.transfersystem.repository.LoanProductRepository loanProductRepository;
    private final com.mycompany.transfersystem.repository.LoanRepository loanRepository;
    private final com.mycompany.transfersystem.repository.LoanRepaymentScheduleRepository scheduleRepository;
    private final com.mycompany.transfersystem.repository.CreditProfileRepository creditProfileRepository;
    private final UserRepository userRepository;

    public LoanController(CreditScoringService creditScoringService,
                          LoanApplicationService loanApplicationService,
                          LoanDisbursementService loanDisbursementService,
                          LoanRepaymentService loanRepaymentService,
                          LoanRiskService loanRiskService,
                          com.mycompany.transfersystem.repository.LoanProductRepository loanProductRepository,
                          com.mycompany.transfersystem.repository.LoanRepository loanRepository,
                          com.mycompany.transfersystem.repository.LoanRepaymentScheduleRepository scheduleRepository,
                          com.mycompany.transfersystem.repository.CreditProfileRepository creditProfileRepository,
                          UserRepository userRepository) {
        this.creditScoringService = creditScoringService;
        this.loanApplicationService = loanApplicationService;
        this.loanDisbursementService = loanDisbursementService;
        this.loanRepaymentService = loanRepaymentService;
        this.loanRiskService = loanRiskService;
        this.loanProductRepository = loanProductRepository;
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.creditProfileRepository = creditProfileRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-credit-score")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreditScoreResponse> getMyCreditScore(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        CreditProfile profile = creditScoringService.calculateAndSave(user.getId());
        return ResponseEntity.ok(CreditScoreResponse.builder()
                .creditScore(profile.getCreditScore())
                .riskTier(profile.getRiskTier())
                .maxLoanAmountUsd(profile.getMaxLoanAmountUsd())
                .calculatedAt(profile.getCalculatedAt())
                .nextReviewAt(profile.getNextReviewAt())
                .scoreComponents(profile.getScoreComponents())
                .build());
    }

    @GetMapping("/products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<com.mycompany.transfersystem.entity.LoanProduct>> getProductsForMyTier(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        CreditProfile profile = creditScoringService.calculateAndSave(user.getId());
        if ("INELIGIBLE".equals(profile.getRiskTier())) {
            return ResponseEntity.ok(List.of());
        }
        List<com.mycompany.transfersystem.entity.LoanProduct> products =
                loanProductRepository.findByRiskTierAndActiveTrue(profile.getRiskTier());
        return ResponseEntity.ok(products);
    }

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoanApplication> apply(@Valid @RequestBody LoanApplicationRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        com.mycompany.transfersystem.entity.LoanProduct product = loanProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        LoanApplication app = loanApplicationService.apply(user.getId(), product, request.getAmount(),
                request.getTermDays(), request.getPurpose(), user);
        return ResponseEntity.ok(app);
    }

    @GetMapping("/my-loans")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LoanResponse>> getMyLoans(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        List<Loan> loans = loanRepository.findByUser_IdOrderByDisbursedAtDesc(user.getId());
        return ResponseEntity.ok(loans.stream().map(this::toLoanResponse).collect(Collectors.toList()));
    }

    @GetMapping("/my-loans/{id}/schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RepaymentScheduleResponse> getSchedule(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        Loan loan = loanRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        if (!loan.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        List<LoanRepaymentSchedule> rows = scheduleRepository.findByLoan_IdOrderByInstalmentNumber(id);
        List<RepaymentScheduleItemDto> items = rows.stream().map(s -> RepaymentScheduleItemDto.builder()
                .instalmentNumber(s.getInstalmentNumber())
                .dueDate(s.getDueDate())
                .principalDue(s.getPrincipalDue())
                .interestDue(s.getInterestDue())
                .totalDue(s.getTotalDue())
                .status(s.getStatus())
                .build()).collect(Collectors.toList());
        return ResponseEntity.ok(RepaymentScheduleResponse.builder().loanId(id).instalments(items).build());
    }

    @PostMapping("/my-loans/{id}/repay")
    @RequireIdempotencyKey
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> repay(@PathVariable Long id,
                                      @Valid @RequestBody ManualRepaymentRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        loanRepaymentService.repay(id, request.getAmount(), request.getScheduleId(), user, "WALLET_MANUAL");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/applications")
    @PreAuthorize("hasAnyRole('MOTHER_BRANCH_ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<List<LoanApplication>> getPendingApplications() {
        return ResponseEntity.ok(loanApplicationService.getPendingApplications());
    }

    @PutMapping("/admin/applications/{appId}/approve")
    @PreAuthorize("hasRole('MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<LoanApplication> approveApplication(@PathVariable Long appId,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        User admin = SecurityUtils.resolveUser(userDetails, userRepository);
        LoanApplication app = loanApplicationService.approve(appId, admin);
        return ResponseEntity.ok(app);
    }

    @PutMapping("/admin/applications/{appId}/reject")
    @PreAuthorize("hasRole('MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<LoanApplication> rejectApplication(@PathVariable Long appId,
                                                            @RequestParam(required = false) String reason,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = SecurityUtils.resolveUser(userDetails, userRepository);
        LoanApplication app = loanApplicationService.reject(appId, reason, admin);
        return ResponseEntity.ok(app);
    }

    @PostMapping("/admin/applications/{appId}/disburse")
    @PreAuthorize("hasRole('MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Loan> disburse(@PathVariable Long appId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        User admin = SecurityUtils.resolveUser(userDetails, userRepository);
        Loan loan = loanDisbursementService.disburse(appId, admin);
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/admin/portfolio")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<List<LoanResponse>> getPortfolio() {
        List<Loan> all = loanRepository.findAll();
        return ResponseEntity.ok(all.stream().map(this::toLoanResponse).collect(Collectors.toList()));
    }

    @GetMapping("/admin/at-risk")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<List<LoanResponse>> getAtRisk() {
        List<Loan> atRisk = loanRiskService.getAtRiskLoans();
        return ResponseEntity.ok(atRisk.stream().map(this::toLoanResponse).collect(Collectors.toList()));
    }

    private LoanResponse toLoanResponse(Loan loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .principalAmount(loan.getPrincipalAmount())
                .currency(loan.getCurrency())
                .disbursedAt(loan.getDisbursedAt())
                .termDays(loan.getTermDays())
                .monthlyPayment(loan.getMonthlyPayment())
                .outstandingBalance(loan.getOutstandingBalance())
                .status(loan.getStatus())
                .build();
    }
}
