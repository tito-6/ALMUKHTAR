package com.mycompany.transfersystem.service.wallet;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.CashOutRequest;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.CashOutRequestRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.cashier.CashierShiftService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class WalletCashOutService {

    private final CashOutRequestRepository cashOutRequestRepository;
    private final WalletRepository walletRepository;
    private final BranchRepository branchRepository;
    private final WalletService walletService;
    private final CashierShiftService cashierShiftService;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public WalletCashOutService(CashOutRequestRepository cashOutRequestRepository,
                                WalletRepository walletRepository,
                                BranchRepository branchRepository,
                                WalletService walletService,
                                CashierShiftService cashierShiftService,
                                AuditService auditService,
                                ApplicationEventPublisher applicationEventPublisher) {
        this.cashOutRequestRepository = cashOutRequestRepository;
        this.walletRepository = walletRepository;
        this.branchRepository = branchRepository;
        this.walletService = walletService;
        this.cashierShiftService = cashierShiftService;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public CashOutRequest requestCashOut(Long walletId, Long branchId, BigDecimal amount, String currency, User user) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not your wallet");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        String ccy = currency != null ? currency : "USD";
        CashOutRequest req = CashOutRequest.builder()
                .wallet(wallet)
                .branch(branch)
                .amount(amount)
                .currency(ccy)
                .status("PENDING")
                .build();
        req = cashOutRequestRepository.save(req);
        auditService.log("WALLET_CASHOUT_REQUESTED", "CASH_OUT_REQUEST", req.getId(),
                "amount=" + amount + " " + ccy, user);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.WalletWithdrawalRequestedEvent(
                req.getId(), user.getId(), walletId, branchId, amount, ccy));
        return req;
    }

    @Transactional
    public CashOutRequest completeCashOut(Long requestId, User cashier) {
        CashOutRequest req = cashOutRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Cash-out request not found"));
        if (!"PENDING".equals(req.getStatus())) {
            throw new IllegalStateException("Cash-out already completed");
        }
        walletService.debit(req.getWallet().getId(), req.getCurrency(), req.getAmount(),
                WalletTransactionType.WITHDRAWAL, "CASHOUT-" + req.getId(), "Branch cash-out");
        req.setStatus("COMPLETED");
        req.setCashier(cashier);
        req.setCompletedAt(Instant.now());
        cashOutRequestRepository.save(req);
        cashierShiftService.recordCashOut(cashier, req.getBranch(), req.getCurrency(), req.getAmount(),
                "CASH_OUT_REQUEST", req.getId(), "Wallet cash-out paid");
        auditService.log("WALLET_CASHOUT_COMPLETED", "CASH_OUT_REQUEST", req.getId(),
                "amount=" + req.getAmount(), cashier);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.WalletWithdrawalCompletedEvent(
                req.getId(), req.getWallet().getUser().getId(), req.getWallet().getId(),
                req.getBranch().getId(), req.getAmount(), req.getCurrency()));
        return req;
    }
}
