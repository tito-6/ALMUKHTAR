package com.mycompany.transfersystem.service.wallet;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.TopupRequest;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.WalletTransaction;
import com.mycompany.transfersystem.entity.WalletBalance;
import com.mycompany.transfersystem.entity.enums.TopupRequestStatus;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.TopupRequestRepository;
import com.mycompany.transfersystem.repository.WalletBalanceRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import com.mycompany.transfersystem.repository.WalletTransactionRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.liquidity.BranchCashService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class WalletTopUpService {

    private final TopupRequestRepository topupRequestRepository;
    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;
    private final BranchCashService branchCashService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public WalletTopUpService(TopupRequestRepository topupRequestRepository,
                              WalletRepository walletRepository,
                              WalletBalanceRepository walletBalanceRepository,
                              WalletTransactionRepository walletTransactionRepository,
                              BranchRepository branchRepository,
                              AuditService auditService,
                              BranchCashService branchCashService,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.topupRequestRepository = topupRequestRepository;
        this.walletRepository = walletRepository;
        this.walletBalanceRepository = walletBalanceRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.branchRepository = branchRepository;
        this.auditService = auditService;
        this.branchCashService = branchCashService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public TopupRequest requestTopUp(Long walletId, Long branchId, BigDecimal amount, String currency, User user) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not your wallet");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        TopupRequest req = TopupRequest.builder()
                .wallet(wallet)
                .branch(branch)
                .amount(amount)
                .currency(currency != null ? currency : "USD")
                .status(TopupRequestStatus.PENDING)
                .build();
        topupRequestRepository.save(req);
        auditService.log("WALLET_TOPUP_REQUESTED", "TOPUP_REQUEST", req.getId(),
                "amount=" + amount + " " + currency, user);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.WalletTopupRequestedEvent(
                req.getId(), user.getId(), wallet.getId(), branchId, amount, req.getCurrency()));
        return req;
    }

    @Transactional
    public TopupRequest completeTopUp(Long requestId, User cashier) {
        TopupRequest req = topupRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Top-up request not found"));
        if (req.getStatus() != TopupRequestStatus.PENDING) {
            throw new IllegalStateException("Top-up already completed or cancelled");
        }

        req.setStatus(TopupRequestStatus.COMPLETED);
        req.setCashier(cashier);
        req.setCompletedAt(Instant.now());
        topupRequestRepository.save(req);

        Wallet wallet = req.getWallet();
        WalletBalance balance = walletBalanceRepository.findByWalletIdAndCurrencyCode(wallet.getId(), req.getCurrency())
                .orElseGet(() -> {
                    WalletBalance wb = WalletBalance.builder()
                            .wallet(wallet)
                            .currencyCode(req.getCurrency())
                            .availableBalance(BigDecimal.ZERO)
                            .lockedBalance(BigDecimal.ZERO)
                            .build();
                    return walletBalanceRepository.save(wb);
                });
        balance.setAvailableBalance(balance.getAvailableBalance().add(req.getAmount()));
        walletBalanceRepository.save(balance);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionType.TOPUP)
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .referenceId("TOPUP-" + req.getId())
                .description("Branch top-up completed")
                .build());

        branchCashService.recordCashIn(req.getBranch(), req.getCurrency(), req.getAmount(), cashier,
                "TOPUP_REQUEST", req.getId(), "Wallet top-up cash received");

        auditService.log("WALLET_TOPUP_COMPLETED", "TOPUP_REQUEST", req.getId(),
                "amount=" + req.getAmount() + " " + req.getCurrency(), cashier);

        BigDecimal newBal = walletBalanceRepository.findByWalletIdAndCurrencyCode(wallet.getId(), req.getCurrency())
                .map(WalletBalance::getAvailableBalance)
                .orElse(null);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.WalletTopupCompletedEvent(
                req.getId(), wallet.getUser().getId(), wallet.getId(), req.getBranch().getId(),
                req.getAmount(), req.getCurrency(), newBal));
        return req;
    }

    public List<TopupRequest> getPendingTopUps(Long branchId) {
        return topupRequestRepository.findByBranchIdAndStatusOrderByCreatedAtAsc(branchId, TopupRequestStatus.PENDING);
    }
}
