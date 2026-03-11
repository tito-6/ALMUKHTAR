package com.mycompany.transfersystem.service.wallet;

import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.WalletApplication;
import com.mycompany.transfersystem.entity.enums.KycTier;
import com.mycompany.transfersystem.entity.enums.WalletApplicationStatus;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.repository.WalletApplicationRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.dto.wallet.KycReviewRequest;
import com.mycompany.transfersystem.dto.wallet.WalletApplicationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WalletApplicationService {

    private final WalletApplicationRepository applicationRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public WalletApplicationService(WalletApplicationRepository applicationRepository,
                                   WalletRepository walletRepository,
                                   UserRepository userRepository,
                                   AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public WalletApplication submitApplication(Long userId, WalletApplicationRequest request, User actor) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        WalletApplication app = applicationRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    WalletApplication newApp = WalletApplication.builder()
                            .user(user)
                            .status(WalletApplicationStatus.DRAFT)
                            .build();
                    return applicationRepository.save(newApp);
                });

        if (app.getStatus() != WalletApplicationStatus.DRAFT && app.getStatus() != WalletApplicationStatus.REJECTED) {
            throw new IllegalStateException("Application already submitted or in review");
        }

        app.setStatus(WalletApplicationStatus.SUBMITTED);
        app.setSubmittedAt(Instant.now());
        applicationRepository.save(app);

        auditService.log("KYC_APPLICATION_SUBMITTED", "WALLET_APPLICATION", app.getId(), "Submitted by user " + userId, actor);
        return app;
    }

    @Transactional
    public WalletApplication reviewApplication(Long applicationId, KycReviewRequest request, User reviewer) {
        WalletApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet application not found: " + applicationId));

        if (app.getStatus() != WalletApplicationStatus.SUBMITTED && app.getStatus() != WalletApplicationStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Application is not pending review");
        }

        app.setReviewedBy(reviewer);
        app.setReviewedAt(Instant.now());

        if (Boolean.TRUE.equals(request.getApproved())) {
            app.setStatus(WalletApplicationStatus.APPROVED);
            app.setRejectionReason(null);
            auditService.log("KYC_APPROVED", "WALLET_APPLICATION", app.getId(), "Approved by " + reviewer.getUsername(), reviewer);
            createWalletIfNotExists(app);
        } else {
            app.setStatus(WalletApplicationStatus.REJECTED);
            app.setRejectionReason(request.getRejectionReason() != null ? request.getRejectionReason() : request.getNotes());
            auditService.log("KYC_REJECTED", "WALLET_APPLICATION", app.getId(),
                    "Rejected: " + app.getRejectionReason(), reviewer);
        }

        return applicationRepository.save(app);
    }

    private void createWalletIfNotExists(WalletApplication app) {
        if (walletRepository.findByUser_Id(app.getUser().getId()).isPresent()) {
            return;
        }
        Wallet wallet = Wallet.builder()
                .user(app.getUser())
                .walletNumber(UUID.randomUUID().toString())
                .status(com.mycompany.transfersystem.entity.enums.WalletStatus.ACTIVE)
                .kycTier(KycTier.STANDARD)
                .dailyLimit(java.math.BigDecimal.valueOf(10000))
                .monthlyLimit(java.math.BigDecimal.valueOf(100000))
                .build();
        walletRepository.save(wallet);
        auditService.log("WALLET_CREATED", "WALLET", wallet.getId(), "Wallet created post-KYC approval", app.getReviewedBy());
    }

    public WalletApplication getMyApplication(Long userId) {
        return applicationRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No wallet application found for user"));
    }

    public java.util.List<WalletApplication> getPendingReviews() {
        return applicationRepository.findByStatusOrderBySubmittedAtDesc(WalletApplicationStatus.SUBMITTED);
    }
}
