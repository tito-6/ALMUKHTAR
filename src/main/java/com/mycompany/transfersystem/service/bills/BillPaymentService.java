package com.mycompany.transfersystem.service.bills;

import com.mycompany.transfersystem.entity.BillPaymentRequest;
import com.mycompany.transfersystem.entity.BillProvider;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.repository.BillPaymentRequestRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.revenue.PlatformRevenueService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class BillPaymentService {

    private final BillPaymentRequestRepository requestRepository;
    private final BillProviderService billProviderService;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final PlatformRevenueService platformRevenueService;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public BillPaymentService(BillPaymentRequestRepository requestRepository,
                              BillProviderService billProviderService,
                              WalletService walletService,
                              WalletRepository walletRepository,
                              PlatformRevenueService platformRevenueService,
                              AuditService auditService,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.requestRepository = requestRepository;
        this.billProviderService = billProviderService;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.platformRevenueService = platformRevenueService;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public BillPaymentRequest payBill(Long providerId, String accountReference, BigDecimal amount, String currency, Long userId, User actor) {
        BillProvider provider = billProviderService.getById(providerId);
        Wallet wallet = walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (currency == null || currency.isBlank()) currency = "USD";
        BigDecimal feePct = provider.getProcessingFeePct() != null ? provider.getProcessingFeePct() : BigDecimal.ZERO;
        BigDecimal platformFee = amount.multiply(feePct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(platformFee);

        if ("DIRECT_API".equals(provider.getApiIntegration())) {
            walletService.debit(wallet.getId(), currency, totalDebit,
                    WalletTransactionType.BILL_PAYMENT, "BILL-" + providerId + "-" + System.currentTimeMillis(), "Bill payment: " + provider.getName());
            if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
                platformRevenueService.collect("BILL_PAYMENT_FEE", platformFee, currency, providerId, "BILL_PROVIDER");
            }
            BillPaymentRequest req = BillPaymentRequest.builder()
                    .user(actor)
                    .provider(provider)
                    .accountReference(accountReference)
                    .amount(amount)
                    .currency(currency)
                    .status("PAID")
                    .platformFee(platformFee)
                    .paidAt(Instant.now())
                    .paymentRef("API-" + System.currentTimeMillis())
                    .build();
            req = requestRepository.save(req);
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.BillPaymentCreatedEvent(
                    req.getId(), actor.getId(), provider.getId(), provider.getName(), false,
                    amount, currency, platformFee));
            auditService.log("BILL_PAYMENT_COMPLETED", "BILL_PAYMENT_REQUEST", req.getId(), "amount=" + amount, actor);
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.BillPaymentCompletedEvent(
                    req.getId(), actor.getId(), "PAID", amount, currency, req.getPaymentRef()));
            return req;
        }

        BillPaymentRequest req = BillPaymentRequest.builder()
                .user(actor)
                .provider(provider)
                .accountReference(accountReference)
                .amount(amount)
                .currency(currency)
                .status("PROCESSING")
                .platformFee(platformFee)
                .build();
        req = requestRepository.save(req);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.BillPaymentCreatedEvent(
                req.getId(), actor.getId(), provider.getId(), provider.getName(), true,
                amount, currency, platformFee));
        walletService.debit(wallet.getId(), currency, totalDebit,
                WalletTransactionType.BILL_PAYMENT, "BILL-PENDING-" + req.getId(), "Bill payment (processing): " + provider.getName());
        auditService.log("BILL_PAYMENT_SUBMITTED", "BILL_PAYMENT_REQUEST", req.getId(), "amount=" + amount + " manual flow", actor);
        return req;
    }

    @Transactional
    public BillPaymentRequest completeManualPayment(Long requestId, User cashier) {
        BillPaymentRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill payment request not found"));
        if (!"PROCESSING".equals(req.getStatus())) {
            throw new IllegalStateException("Request is not in PROCESSING status");
        }
        req.setStatus("PAID");
        req.setPaidAt(Instant.now());
        req.setPaymentRef("CASHIER-" + cashier.getId() + "-" + System.currentTimeMillis());
        req = requestRepository.save(req);
        if (req.getPlatformFee().compareTo(BigDecimal.ZERO) > 0) {
            platformRevenueService.collect("BILL_PAYMENT_FEE", req.getPlatformFee(), req.getCurrency(), req.getProvider().getId(), "BILL_PROVIDER");
        }
        auditService.log("BILL_PAYMENT_COMPLETED", "BILL_PAYMENT_REQUEST", req.getId(), "Completed by cashier", cashier);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.BillPaymentCompletedEvent(
                req.getId(), req.getUser().getId(), "PAID", req.getAmount(), req.getCurrency(), req.getPaymentRef()));
        return req;
    }

    @Transactional(readOnly = true)
    public List<BillPaymentRequest> getMyHistory(Long userId) {
        return requestRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<BillPaymentRequest> getPendingManual() {
        return requestRepository.findByStatus("PROCESSING");
    }
}
