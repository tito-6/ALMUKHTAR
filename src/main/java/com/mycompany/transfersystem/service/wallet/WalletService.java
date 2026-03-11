package com.mycompany.transfersystem.service.wallet;

import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.CurrencyConversionService;
import com.mycompany.transfersystem.service.accounting.AccountingLedgerService;
import com.mycompany.transfersystem.entity.enums.CommissionScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CurrencyConversionService currencyConversionService;
    private final CommissionRateRepository commissionRateRepository;
    private final BranchRepository branchRepository;
    private final AccountingLedgerService accountingLedgerService;
    private final AuditService auditService;

    public WalletService(WalletRepository walletRepository,
                         WalletBalanceRepository walletBalanceRepository,
                         WalletTransactionRepository walletTransactionRepository,
                         CurrencyConversionService currencyConversionService,
                         CommissionRateRepository commissionRateRepository,
                         BranchRepository branchRepository,
                         AccountingLedgerService accountingLedgerService,
                         AuditService auditService) {
        this.walletRepository = walletRepository;
        this.walletBalanceRepository = walletBalanceRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.currencyConversionService = currencyConversionService;
        this.commissionRateRepository = commissionRateRepository;
        this.branchRepository = branchRepository;
        this.accountingLedgerService = accountingLedgerService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user"));
    }

    @Transactional(readOnly = true)
    public List<WalletBalance> getBalances(Long walletId) {
        return walletBalanceRepository.findByWalletIdOrderByCurrencyCode(walletId);
    }

    @Transactional
    public void exchangeCurrency(Long walletId, String fromCurrency, String toCurrency,
                                  BigDecimal amount, User actor) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (!wallet.getUser().getId().equals(actor.getId())) {
            throw new SecurityException("Not your wallet");
        }

        WalletBalance fromBalance = walletBalanceRepository.findByWalletIdAndCurrencyCodeForUpdate(walletId, fromCurrency)
                .orElseThrow(() -> new ResourceNotFoundException("Balance not found for " + fromCurrency));

        if (fromBalance.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        BigDecimal platformFee = getWalletExchangeFee(amount, fromCurrency);
        BigDecimal totalDebit = amount.add(platformFee);

        if (fromBalance.getAvailableBalance().compareTo(totalDebit) < 0) {
            throw new IllegalStateException("Insufficient balance for amount + fee");
        }

        BigDecimal convertedAmount = currencyConversionService.convertCurrency(amount, fromCurrency, toCurrency);

        fromBalance.setAvailableBalance(fromBalance.getAvailableBalance().subtract(totalDebit));
        walletBalanceRepository.save(fromBalance);

        WalletBalance toBalance = walletBalanceRepository.findByWalletIdAndCurrencyCodeForUpdate(walletId, toCurrency)
                .orElseGet(() -> {
                    WalletBalance nb = WalletBalance.builder()
                            .wallet(wallet)
                            .currencyCode(toCurrency)
                            .availableBalance(BigDecimal.ZERO)
                            .lockedBalance(BigDecimal.ZERO)
                            .build();
                    return walletBalanceRepository.save(nb);
                });
        toBalance.setAvailableBalance(toBalance.getAvailableBalance().add(convertedAmount));
        walletBalanceRepository.save(toBalance);

        long txId = System.currentTimeMillis();
        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionType.EXCHANGE)
                .amount(amount.negate())
                .currency(fromCurrency)
                .referenceId("EX-" + txId)
                .description("Exchange to " + toCurrency)
                .build());
        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionType.EXCHANGE)
                .amount(convertedAmount)
                .currency(toCurrency)
                .referenceId("EX-" + txId)
                .description("Exchange from " + fromCurrency)
                .build());
        if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
            walletTransactionRepository.save(WalletTransaction.builder()
                    .wallet(wallet)
                    .type(WalletTransactionType.TRADE_FEE)
                    .amount(platformFee.negate())
                    .currency(fromCurrency)
                    .referenceId("FEE-" + txId)
                    .description("Wallet exchange fee")
                    .build());
        }

        accountingLedgerService.postDoubleEntryForReference(
                "WALLET_EXCHANGE", txId,
                "WALLET_" + walletId + "_" + fromCurrency,
                "WALLET_" + walletId + "_" + toCurrency,
                amount, fromCurrency,
                "Exchange " + fromCurrency + " -> " + toCurrency, actor);
        if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
            accountingLedgerService.postDoubleEntryForReference(
                    "WALLET_EXCHANGE_FEE", txId,
                    "WALLET_" + walletId + "_" + fromCurrency,
                    "PLATFORM_OWNER_REVENUE",
                    platformFee, fromCurrency,
                    "Wallet exchange fee", actor);
        }

        auditService.log("WALLET_EXCHANGE", "WALLET", walletId,
                String.format("from=%s to=%s amount=%s fee=%s", fromCurrency, toCurrency, amount, platformFee), actor);
    }

    private BigDecimal getWalletExchangeFee(BigDecimal amount, String currency) {
        Branch mainBranch = branchRepository.findFirstByName("MAIN_ADMIN_BRANCH").orElse(null);
        if (mainBranch == null) return BigDecimal.ZERO;
        return commissionRateRepository.findByBranchAndCommissionScope(mainBranch, CommissionScope.WALLET_EXCHANGE)
                .map(CommissionRate::getRateValue)
                .map(rate -> amount.multiply(rate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<WalletTransaction> getTransactionHistory(Long walletId, org.springframework.data.domain.Pageable pageable) {
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
    }

    /**
     * Debit (deduct) amount from a wallet's balance. Uses pessimistic lock.
     * Caller must be in a transaction. Does not check wallet ownership.
     */
    @Transactional
    public WalletTransaction debit(Long walletId, String currency, BigDecimal amount,
                                    WalletTransactionType type, String referenceId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (wallet.getStatus() != com.mycompany.transfersystem.entity.enums.WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active for debit: " + wallet.getStatus());
        }
        WalletBalance balance = walletBalanceRepository.findByWalletIdAndCurrencyCodeForUpdate(walletId, currency)
                .orElseThrow(() -> new ResourceNotFoundException("Balance not found for " + currency));
        BigDecimal available = balance.getAvailableBalance();
        if (available.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance: required " + amount + ", available " + available);
        }
        balance.setAvailableBalance(available.subtract(amount));
        walletBalanceRepository.save(balance);
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount.negate())
                .currency(currency)
                .referenceId(referenceId != null ? referenceId : "DB-" + System.currentTimeMillis())
                .description(description)
                .build();
        return walletTransactionRepository.save(tx);
    }

    /**
     * Credit (add) amount to a wallet's balance. Uses pessimistic lock.
     * Caller must be in a transaction. Creates balance row if missing.
     */
    @Transactional
    public WalletTransaction credit(Long walletId, String currency, BigDecimal amount,
                                     WalletTransactionType type, String referenceId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        WalletBalance balance = walletBalanceRepository.findByWalletIdAndCurrencyCodeForUpdate(walletId, currency)
                .orElseGet(() -> {
                    WalletBalance wb = WalletBalance.builder()
                            .wallet(wallet)
                            .currencyCode(currency)
                            .availableBalance(BigDecimal.ZERO)
                            .lockedBalance(BigDecimal.ZERO)
                            .build();
                    return walletBalanceRepository.save(wb);
                });
        balance.setAvailableBalance(balance.getAvailableBalance().add(amount));
        walletBalanceRepository.save(balance);
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .currency(currency)
                .referenceId(referenceId != null ? referenceId : "CR-" + System.currentTimeMillis())
                .description(description)
                .build();
        return walletTransactionRepository.save(tx);
    }

    /**
     * Freeze wallet (e.g. loan default, escrow dispute). Sets status to FROZEN.
     */
    @Transactional
    public void freezeWallet(Long walletId) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        wallet.setStatus(com.mycompany.transfersystem.entity.enums.WalletStatus.FROZEN);
        walletRepository.save(wallet);
    }

    /**
     * Unfreeze wallet. Sets status to ACTIVE.
     */
    @Transactional
    public void unfreezeWallet(Long walletId) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        wallet.setStatus(com.mycompany.transfersystem.entity.enums.WalletStatus.ACTIVE);
        walletRepository.save(wallet);
    }

    /**
     * Wallet-to-wallet transfer (P2P). Debits sender, credits receiver. Same currency.
     * Caller must ensure both wallets exist and sender has sufficient balance.
     */
    @Transactional
    public WalletTransaction transferP2P(Long senderWalletId, Long receiverWalletId, String currency,
                                           BigDecimal amount, String referenceId, String description) {
        WalletTransaction debitTx = debit(senderWalletId, currency, amount,
                WalletTransactionType.TRANSFER, referenceId, description);
        credit(receiverWalletId, currency, amount,
                WalletTransactionType.TRANSFER, referenceId, description != null ? description : "P2P transfer");
        return debitTx;
    }
}
