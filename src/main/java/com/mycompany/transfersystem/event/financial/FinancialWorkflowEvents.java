package com.mycompany.transfersystem.event.financial;

import java.math.BigDecimal;
import java.time.Instant;
/**
 * Domain events for financial workflows. Published from transactional services;
 * consumers should use {@link org.springframework.transaction.event.TransactionalEventListener}
 * with {@link org.springframework.transaction.event.TransactionPhase#AFTER_COMMIT} unless noted.
 */
public final class FinancialWorkflowEvents {

    private FinancialWorkflowEvents() {}

    public record TransferCreatedEvent(
            Long transactionId,
            Long senderId,
            Long receiverId,
            BigDecimal amount,
            String currency,
            BigDecimal totalFees,
            Long senderBranchId,
            Long receiverBranchId,
            String status,
            boolean payoutReady,
            boolean manualBranchActionRequired,
            boolean highValueForPlatformSummary
    ) {}

    /** Replaces legacy {@code TransactionCompletedEvent} for transfers (same lifecycle hook). */
    public record TransferCompletedEvent(
            Long transactionId,
            Long senderId,
            Long receiverId,
            BigDecimal amount,
            String currency,
            BigDecimal totalFees,
            Long senderBranchId,
            Long receiverBranchId,
            boolean branchCashMateriallyChanged,
            boolean highValueForPlatformSummary
    ) {}

    public record TransferFailedEvent(Long transactionId, Long senderId, Long receiverId, String reasonCode) {}

    public record TransferCancelledEvent(Long transactionId, Long senderId, Long receiverId, String reasonCode) {}

    /** Receiver may collect funds at branch; funds and reservation are in place. */
    public record TransferReadyForPickupEvent(
            Long transactionId,
            Long senderId,
            Long receiverId,
            java.math.BigDecimal amount,
            String currency,
            java.math.BigDecimal totalFees,
            Long senderBranchId,
            Long receiverBranchId,
            boolean highValueForPlatformSummary
    ) {}

    /** Physical payout completed (QR scan or passcode release at counter). */
    public record TransferReleasedEvent(
            Long transactionId,
            Long senderId,
            Long receiverId,
            java.math.BigDecimal amount,
            String currency,
            java.math.BigDecimal totalFees,
            Long senderBranchId,
            Long receiverBranchId,
            String releaseChannel,
            Long payoutActorUserId
    ) {}

    public record QrReleaseCreatedEvent(
            Long transactionId,
            Long senderId,
            Long receiverId,
            Instant qrExpiresAt,
            boolean warnDoNotShare
    ) {}

    public record QrReleaseCompletedEvent(
            Long transactionId,
            Long senderId,
            Long receiverId,
            Long cashierUserId,
            String releaseChannel
    ) {}

    public record WalletTopupRequestedEvent(Long topupRequestId, Long userId, Long walletId, Long branchId,
                                            BigDecimal amount, String currency) {}

    public record WalletTopupCompletedEvent(Long topupRequestId, Long userId, Long walletId, Long branchId,
                                            BigDecimal amount, String currency, BigDecimal newBalanceSameCurrency) {}

    public record WalletWithdrawalRequestedEvent(Long cashOutRequestId, Long userId, Long walletId, Long branchId,
                                                 BigDecimal amount, String currency) {}

    public record WalletWithdrawalCompletedEvent(Long cashOutRequestId, Long userId, Long walletId, Long branchId,
                                                 BigDecimal amount, String currency) {}

    public record MerchantPaymentCompletedEvent(
            Long merchantTransactionId,
            Long payerUserId,
            Long merchantOwnerUserId,
            Long merchantId,
            String merchantDisplayName,
            BigDecimal amount,
            String currency,
            BigDecimal platformFee,
            BigDecimal merchantNet,
            String paymentRef,
            boolean highValuePerTradePlatformNotify
    ) {}

    public record BillPaymentCreatedEvent(Long billPaymentRequestId, Long userId, Long providerId,
                                          String providerName, boolean manualQueue, BigDecimal amount,
                                          String currency, BigDecimal platformFee) {}

    public record BillPaymentCompletedEvent(Long billPaymentRequestId, Long userId, String status,
                                            BigDecimal amount, String currency, String paymentRef) {}

    public record BatchJobSubmittedEvent(Long batchJobId, Long submitterUserId, int rowCount, BigDecimal totalAmountUsd,
                                         boolean pendingApproval) {}

    public record BatchJobCompletedEvent(Long batchJobId, Long submitterUserId, int successCount, int failedCount) {}

    public record LoanPaymentDueEvent(Long loanId, Long userId, Long scheduleId, BigDecimal amountDue,
                                      String currency, String dueDate) {}

    public record LoanPaymentFailedEvent(Long loanId, Long userId, Long scheduleId, String reasonCode,
                                         BigDecimal lateFee, boolean escalatedDefault) {}

    public record EscrowFundedEvent(Long contractId, Long initiatorUserId, Long beneficiaryUserId,
                                    BigDecimal amount, String currency, String conditionType) {}

    public record EscrowReleasedEvent(Long contractId, Long initiatorUserId, Long beneficiaryUserId,
                                      BigDecimal amount, String currency) {}

    public record EscrowDisputedEvent(Long contractId, Long initiatorUserId, Long beneficiaryUserId,
                                      String reasonCategory, String conditionType) {}

    public record TradingOrderPlacedEvent(Long orderId, Long tradingAccountId, Long userId, String symbol,
                                          String side, BigDecimal quantity, BigDecimal indicatedFeeUsd) {}

    public record TradingOrderFilledEvent(Long orderId, Long tradingAccountId, Long userId, String symbol,
                                          String side, BigDecimal quantity, BigDecimal fillPrice, BigDecimal feeUsd,
                                          boolean highValuePerTradePlatformNotify) {}

    public record TradingOrderFailedEvent(Long tradingAccountId, Long userId, String symbol, String reasonCode) {}

    public record AmlAlertCreatedEvent(Long alertId, Long subjectUserId, String severity, String summaryForStaff) {}

    public record LiquidityLowEvent(Long branchId, String branchName, String currency, BigDecimal available,
                                    BigDecimal lowThreshold) {}

    public record AccountFrozenEvent(Long walletId, Long userId, String caseId, String reasonCategory,
                                     String customerVisibleReason, String appealUrl) {}

    public record DisputeOpenedEvent(Long disputeId, Long reporterUserId, Long transactionId, String category) {}

    public record DisputeResolvedEvent(Long disputeId, Long reporterUserId, String resolutionSummary) {}
}
