package com.mycompany.transfersystem.event.listener;

import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.notification.FinancialNotificationPublisher;
import com.mycompany.transfersystem.notification.NotificationRecipientPolicy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers WhatsApp + in-app notifications after successful DB commit for financial workflows.
 */
@Component
public class FinancialWorkflowNotificationListener {

    private final NotificationRecipientPolicy policy;
    private final FinancialNotificationPublisher publisher;

    public FinancialWorkflowNotificationListener(NotificationRecipientPolicy policy,
                                               FinancialNotificationPublisher publisher) {
        this.policy = policy;
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onTransferCreated(FinancialWorkflowEvents.TransferCreatedEvent e) {
        publisher.publishAll(policy.forTransferCreated(e), "TransferCreatedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onTransferReadyForPickup(FinancialWorkflowEvents.TransferReadyForPickupEvent e) {
        publisher.publishAll(policy.forTransferReadyForPickup(e), "TransferReadyForPickupEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onTransferReleased(FinancialWorkflowEvents.TransferReleasedEvent e) {
        publisher.publishAll(policy.forTransferReleased(e), "TransferReleasedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onTransferCompleted(FinancialWorkflowEvents.TransferCompletedEvent e) {
        publisher.publishAll(policy.forTransferCompleted(e), "TransferCompletedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onTransferFailed(FinancialWorkflowEvents.TransferFailedEvent e) {
        publisher.publishAll(policy.forTransferFailed(e), "TransferFailedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onTransferCancelled(FinancialWorkflowEvents.TransferCancelledEvent e) {
        publisher.publishAll(policy.forTransferCancelled(e), "TransferCancelledEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onQrReleaseCreated(FinancialWorkflowEvents.QrReleaseCreatedEvent e) {
        publisher.publishAll(policy.forQrCreated(e), "QrReleaseCreatedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onWalletTopupRequested(FinancialWorkflowEvents.WalletTopupRequestedEvent e) {
        publisher.publishAll(policy.forWalletTopupRequested(e), "WalletTopupRequestedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onWalletTopupCompleted(FinancialWorkflowEvents.WalletTopupCompletedEvent e) {
        publisher.publishAll(policy.forWalletTopupCompleted(e), "WalletTopupCompletedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onWalletWithdrawalRequested(FinancialWorkflowEvents.WalletWithdrawalRequestedEvent e) {
        publisher.publishAll(policy.forWalletWithdrawalRequested(e), "WalletWithdrawalRequestedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onWalletWithdrawalCompleted(FinancialWorkflowEvents.WalletWithdrawalCompletedEvent e) {
        publisher.publishAll(policy.forWalletWithdrawalCompleted(e), "WalletWithdrawalCompletedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onMerchantPaymentCompleted(FinancialWorkflowEvents.MerchantPaymentCompletedEvent e) {
        publisher.publishAll(policy.forMerchantPayment(e), "MerchantPaymentCompletedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onBillPaymentCreated(FinancialWorkflowEvents.BillPaymentCreatedEvent e) {
        publisher.publishAll(policy.forBillCreated(e), "BillPaymentCreatedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onBillPaymentCompleted(FinancialWorkflowEvents.BillPaymentCompletedEvent e) {
        publisher.publishAll(policy.forBillCompleted(e), "BillPaymentCompletedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onBatchJobSubmitted(FinancialWorkflowEvents.BatchJobSubmittedEvent e) {
        publisher.publishAll(policy.forBatchSubmitted(e), "BatchJobSubmittedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onBatchJobCompleted(FinancialWorkflowEvents.BatchJobCompletedEvent e) {
        publisher.publishAll(policy.forBatchCompleted(e), "BatchJobCompletedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onLoanPaymentDue(FinancialWorkflowEvents.LoanPaymentDueEvent e) {
        publisher.publishAll(policy.forLoanDue(e), "LoanPaymentDueEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onLoanPaymentFailed(FinancialWorkflowEvents.LoanPaymentFailedEvent e) {
        publisher.publishAll(policy.forLoanPaymentFailed(e), "LoanPaymentFailedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onEscrowFunded(FinancialWorkflowEvents.EscrowFundedEvent e) {
        publisher.publishAll(policy.forEscrowFunded(e), "EscrowFundedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onEscrowReleased(FinancialWorkflowEvents.EscrowReleasedEvent e) {
        publisher.publishAll(policy.forEscrowReleased(e), "EscrowReleasedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onEscrowDisputed(FinancialWorkflowEvents.EscrowDisputedEvent e) {
        publisher.publishAll(policy.forEscrowDisputed(e), "EscrowDisputedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onTradingOrderPlaced(FinancialWorkflowEvents.TradingOrderPlacedEvent e) {
        publisher.publishAll(policy.forTradingPlaced(e), "TradingOrderPlacedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onTradingOrderFilled(FinancialWorkflowEvents.TradingOrderFilledEvent e) {
        publisher.publishAll(policy.forTradingFilled(e), "TradingOrderFilledEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    @Async("notificationExecutor")
    public void onTradingOrderFailed(FinancialWorkflowEvents.TradingOrderFailedEvent e) {
        publisher.publishAll(policy.forTradingFailed(e), "TradingOrderFailedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onAmlAlertCreated(FinancialWorkflowEvents.AmlAlertCreatedEvent e) {
        publisher.publishAll(policy.forAmlAlertCreated(e), "AmlAlertCreatedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onLiquidityLow(FinancialWorkflowEvents.LiquidityLowEvent e) {
        publisher.publishAll(policy.forLiquidityLow(e), "LiquidityLowEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onAccountFrozen(FinancialWorkflowEvents.AccountFrozenEvent e) {
        publisher.publishAll(policy.forAccountFrozen(e), "AccountFrozenEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onDisputeOpened(FinancialWorkflowEvents.DisputeOpenedEvent e) {
        publisher.publishAll(policy.forDisputeOpened(e), "DisputeOpenedEvent");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onDisputeResolved(FinancialWorkflowEvents.DisputeResolvedEvent e) {
        publisher.publishAll(policy.forDisputeResolved(e), "DisputeResolvedEvent");
    }
}
