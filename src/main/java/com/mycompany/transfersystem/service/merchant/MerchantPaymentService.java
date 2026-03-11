package com.mycompany.transfersystem.service.merchant;

import com.mycompany.transfersystem.entity.Merchant;
import com.mycompany.transfersystem.entity.MerchantTransaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.MerchantRepository;
import com.mycompany.transfersystem.repository.MerchantTransactionRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.revenue.PlatformRevenueService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class MerchantPaymentService {

    private final MerchantRepository merchantRepository;
    private final MerchantOnboardingService merchantOnboardingService;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final MerchantTransactionRepository transactionRepository;
    private final PlatformRevenueService platformRevenueService;
    private final AuditService auditService;

    public MerchantPaymentService(MerchantRepository merchantRepository,
                                  MerchantOnboardingService merchantOnboardingService,
                                  WalletService walletService,
                                  WalletRepository walletRepository,
                                  MerchantTransactionRepository transactionRepository,
                                  PlatformRevenueService platformRevenueService,
                                  AuditService auditService) {
        this.merchantRepository = merchantRepository;
        this.merchantOnboardingService = merchantOnboardingService;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.platformRevenueService = platformRevenueService;
        this.auditService = auditService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MerchantTransaction processPayment(Long merchantId, Long payerUserId, BigDecimal amount,
                                              String currency, String description, String qrScanRef, User payer) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found: " + merchantId));
        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new IllegalStateException("Merchant is not active");
        }
        Wallet payerWallet = walletRepository.findByUser_Id(payerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Payer wallet not found"));
        Wallet merchantWallet = walletRepository.findByUser_Id(merchant.getOwnerUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant owner wallet not found"));
        if (currency == null || currency.isBlank()) currency = "USD";

        BigDecimal feePct = merchant.getProcessingFeePct() != null ? merchant.getProcessingFeePct() : new BigDecimal("1.5");
        BigDecimal platformFee = amount.multiply(feePct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal merchantNet = amount.subtract(platformFee);

        walletService.debit(payerWallet.getId(), currency, amount,
                WalletTransactionType.MERCHANT_DEBIT, "MERCH-" + merchantId + "-" + Instant.now().toEpochMilli(), description != null ? description : "Merchant payment");
        walletService.credit(merchantWallet.getId(), currency, merchantNet,
                WalletTransactionType.MERCHANT_CREDIT, "MERCH-" + merchantId + "-" + Instant.now().toEpochMilli(), description != null ? description : "Merchant sale");

        if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
            platformRevenueService.collect("MERCHANT_PROCESSING_FEE", platformFee, currency, merchantId, "MERCHANT");
        }

        MerchantTransaction tx = MerchantTransaction.builder()
                .merchant(merchant)
                .payerUser(payer)
                .amount(amount)
                .currency(currency)
                .description(description)
                .platformFee(platformFee)
                .merchantNetAmount(merchantNet)
                .status("COMPLETED")
                .qrScanRef(qrScanRef)
                .build();
        tx = transactionRepository.save(tx);
        auditService.log("MERCHANT_PAYMENT_PROCESSED", "MERCHANT_TRANSACTION", tx.getId(),
                "amount=" + amount + " merchantId=" + merchantId, payer);
        return tx;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MerchantTransaction processQrPayment(String qrData, Long payerUserId, BigDecimal amount,
                                               String description, User payer) {
        Long merchantId = merchantOnboardingService.decodeMerchantIdFromQr(qrData);
        return processPayment(merchantId, payerUserId, amount, "USD", description, "QR-" + System.currentTimeMillis(), payer);
    }
}
