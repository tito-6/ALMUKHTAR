package com.mycompany.transfersystem.notification;

import com.mycompany.transfersystem.config.NotificationThresholdProperties;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.InAppNotification;
import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.TransactionRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centralizes who receives which financial notification for each workflow event.
 */
@Component
public class NotificationRecipientPolicy {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationThresholdProperties thresholds;

    public NotificationRecipientPolicy(UserRepository userRepository,
                                       BranchRepository branchRepository,
                                       TransactionRepository transactionRepository,
                                       NotificationThresholdProperties thresholds) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.transactionRepository = transactionRepository;
        this.thresholds = thresholds;
    }

    public List<FinancialNotificationDispatch> forTransferCreated(FinancialWorkflowEvents.TransferCreatedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        Transaction tx = transactionRepository.findById(e.transactionId()).orElse(null);
        String ref = "TXN-" + e.transactionId();
        String maskedReceiver = maskUserId(e.receiverId());
        String feeStr = e.totalFees() != null ? e.totalFees().toPlainString() : "0";
        String branchLine = branchLine(e.receiverBranchId());

        out.add(dispatch(
                idem("transfer", "created", "sender", e.transactionId(), e.senderId()),
                e.senderId(),
                NotificationTemplateKeys.TRANSFER_CREATED_SENDER,
                "Transfer " + ref,
                "Reference " + ref + ". Amount " + amountLine(e.amount(), e.currency())
                        + ", fee " + feeStr + " " + nz(e.currency()) + ". Receiver " + maskedReceiver
                        + ". Status " + e.status() + "." + branchLine,
                InAppNotification.NotificationType.TRANSACTION));

        if (e.payoutReady()) {
            out.add(dispatch(
                    idem("transfer", "created", "receiver", e.transactionId(), e.receiverId()),
                    e.receiverId(),
                    NotificationTemplateKeys.TRANSFER_CREATED_RECEIVER,
                    "Pickup available — " + ref,
                    "Reference " + ref + ". Amount " + amountLine(e.amount(), e.currency())
                            + ". Pickup is ready at branch. " + branchLine,
                    InAppNotification.NotificationType.TRANSACTION,
                    true));
        } else {
            out.add(dispatch(
                    idem("transfer", "created", "receiver_pending", e.transactionId(), e.receiverId()),
                    e.receiverId(),
                    NotificationTemplateKeys.TRANSFER_CREATED_RECEIVER,
                    "Transfer in progress — " + ref,
                    "Reference " + ref + ". Status " + e.status() + ". You will be notified when payout is ready."
                            + (e.receiverBranchId() != null ? " Branch ref: #" + e.receiverBranchId() + "." : ""),
                    InAppNotification.NotificationType.TRANSACTION,
                    true));
        }

        if (e.manualBranchActionRequired() && e.receiverBranchId() != null) {
            for (Long uid : distinctUserIds(branchStaff(e.receiverBranchId()))) {
                out.add(dispatch(
                        idem("transfer", "created", "branch_action", e.transactionId(), uid),
                        uid,
                        NotificationTemplateKeys.TRANSFER_CREATED_BRANCH_ACTION,
                        "Branch action required — " + ref,
                        "Reference " + ref + ". Manual branch step may be required for payout. Amount "
                                + amountLine(e.amount(), e.currency()) + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }

        if (e.highValueForPlatformSummary()) {
            for (Long uid : platformOwnersAndMother()) {
                out.add(dispatch(
                        idem("transfer", "created", "platform", e.transactionId(), uid),
                        uid,
                        NotificationTemplateKeys.TRANSFER_CREATED_PLATFORM_SUMMARY,
                        "High-value transfer summary — " + ref,
                        "Reference " + ref + ". Amount " + amountLine(e.amount(), e.currency())
                                + ". Sender id " + e.senderId() + ", receiver id " + e.receiverId() + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }

        if (tx != null) {
            out.addAll(senderBranchInternalAlerts(tx, e.senderBranchId(), ref, e.amount(), e.currency()));
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forTransferCompleted(FinancialWorkflowEvents.TransferCompletedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "TXN-" + e.transactionId();
        String feeStr = e.totalFees() != null ? e.totalFees().toPlainString() : "0";

        out.add(dispatch(
                idem("transfer", "completed", "sender", e.transactionId(), e.senderId()),
                e.senderId(),
                NotificationTemplateKeys.TRANSFER_COMPLETED_SENDER_RECEIPT,
                "Transfer completed — " + ref,
                "Receipt " + ref + ". Amount " + amountLine(e.amount(), e.currency())
                        + ", total fees " + feeStr + " " + nz(e.currency()) + ". Status COMPLETED.",
                InAppNotification.NotificationType.TRANSACTION));

        out.add(dispatch(
                idem("transfer", "completed", "receiver", e.transactionId(), e.receiverId()),
                e.receiverId(),
                NotificationTemplateKeys.TRANSFER_COMPLETED_RECEIVER_PAYOUT,
                "Payout received — " + ref,
                "Confirmation " + ref + ". Amount " + amountLine(e.amount(), e.currency()) + " is available per branch instructions.",
                InAppNotification.NotificationType.TRANSACTION));

        if (e.branchCashMateriallyChanged() && e.receiverBranchId() != null) {
            for (Long uid : distinctUserIds(branchManagers(e.receiverBranchId()))) {
                out.add(dispatch(
                        idem("transfer", "completed", "bm", e.transactionId(), uid),
                        uid,
                        NotificationTemplateKeys.TRANSFER_COMPLETED_BRANCH_CASH,
                        "Branch cash movement — " + ref,
                        "Reference " + ref + ". Receiver branch cash materially changed for payout. Amount "
                                + amountLine(e.amount(), e.currency()) + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }

        if (e.highValueForPlatformSummary()) {
            for (Long uid : platformOwnersAndMother()) {
                out.add(dispatch(
                        idem("transfer", "completed", "platform", e.transactionId(), uid),
                        uid,
                        NotificationTemplateKeys.TRANSFER_COMPLETED_PLATFORM_SUMMARY,
                        "Revenue-related transfer — " + ref,
                        "Summary " + ref + ". Amount " + amountLine(e.amount(), e.currency())
                                + ". See revenue tools for fee allocation.",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forTransferFailed(FinancialWorkflowEvents.TransferFailedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "TXN-" + e.transactionId();
        if (e.senderId() != null) {
            out.add(dispatch(
                    idem("transfer", "failed", "sender", e.transactionId(), e.senderId()),
                    e.senderId(),
                    NotificationTemplateKeys.TRANSFER_FAILED_SENDER,
                    "Transfer failed — " + ref,
                    "Reference " + ref + ". Reason category: " + nz(e.reasonCode()) + ".",
                    InAppNotification.NotificationType.ALERT,
                    true));
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forTransferCancelled(FinancialWorkflowEvents.TransferCancelledEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "TXN-" + e.transactionId();
        for (Long uid : distinctUserIds(List.of(e.senderId(), e.receiverId()))) {
            if (uid == null) continue;
            out.add(dispatch(
                    idem("transfer", "cancelled", ref, uid),
                    uid,
                    NotificationTemplateKeys.TRANSFER_CANCELLED_PARTIES,
                    "Transfer cancelled — " + ref,
                    "Reference " + ref + " was cancelled. " + nz(e.reasonCode()),
                    InAppNotification.NotificationType.ALERT,
                    true));
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forQrCreated(FinancialWorkflowEvents.QrReleaseCreatedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "TXN-" + e.transactionId();
        out.add(dispatch(
                idem("qr", "created", "sender", e.transactionId(), e.senderId()),
                e.senderId(),
                NotificationTemplateKeys.QR_CREATED_SENDER,
                "QR release ready — " + ref,
                "Reference " + ref + ". QR/passcode channel active. Expires " + e.qrExpiresAt()
                        + ". Do not share codes with anyone you do not trust.",
                InAppNotification.NotificationType.TRANSACTION));
        out.add(dispatch(
                idem("qr", "created", "receiver", e.transactionId(), e.receiverId()),
                e.receiverId(),
                NotificationTemplateKeys.QR_CREATED_RECEIVER,
                "Pickup instructions — " + ref,
                "Reference " + ref + ". Visit your payout branch with ID. QR is held by sender; do not request secrets by phone.",
                InAppNotification.NotificationType.TRANSACTION));
        return out;
    }

    public List<FinancialNotificationDispatch> forQrCompleted(FinancialWorkflowEvents.QrReleaseCompletedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "TXN-" + e.transactionId();
        if (e.cashierUserId() != null) {
            out.add(dispatch(
                    idem("qr", "completed", "cashier", e.transactionId(), e.cashierUserId()),
                    e.cashierUserId(),
                    NotificationTemplateKeys.QR_COMPLETED_CASHIER,
                    "QR release success — " + ref,
                    "Reference " + ref + ". Release channel " + nz(e.releaseChannel()) + ".",
                    InAppNotification.NotificationType.TRANSACTION,
                    false));
        }
        out.add(dispatch(
                idem("qr", "completed", "sender", e.transactionId(), e.senderId()),
                e.senderId(),
                NotificationTemplateKeys.QR_COMPLETED_SENDER,
                "Funds released — " + ref,
                "Reference " + ref + ". Receiver collected via " + nz(e.releaseChannel()) + ".",
                InAppNotification.NotificationType.TRANSACTION));
        out.add(dispatch(
                idem("qr", "completed", "receiver", e.transactionId(), e.receiverId()),
                e.receiverId(),
                NotificationTemplateKeys.QR_COMPLETED_RECEIVER,
                "Pickup completed — " + ref,
                "Reference " + ref + ". Thank you for using ALMUKHTAR.",
                InAppNotification.NotificationType.TRANSACTION));
        return out;
    }

    public List<FinancialNotificationDispatch> forTransferReadyForPickup(FinancialWorkflowEvents.TransferReadyForPickupEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "TXN-" + e.transactionId();
        String feeStr = e.totalFees() != null ? e.totalFees().toPlainString() : "0";
        out.add(dispatch(
                idem("transfer", "ready", "receiver", e.transactionId(), e.receiverId()),
                e.receiverId(),
                NotificationTemplateKeys.TRANSFER_CREATED_RECEIVER,
                "Pickup available — " + ref,
                "Reference " + ref + ". Amount " + amountLine(e.amount(), e.currency())
                        + ", fees " + feeStr + " " + nz(e.currency()) + ". Funds are ready for collection at your branch."
                        + branchLine(e.receiverBranchId()),
                InAppNotification.NotificationType.TRANSACTION,
                true));
        if (e.receiverBranchId() != null) {
            for (Long uid : distinctUserIds(branchStaff(e.receiverBranchId()))) {
                out.add(dispatch(
                        idem("transfer", "ready", "cashier", e.transactionId(), uid),
                        uid,
                        NotificationTemplateKeys.TRANSFER_CREATED_BRANCH_ACTION,
                        "Payout queue — " + ref,
                        "Reference " + ref + ". Receiver pickup ready. Amount " + amountLine(e.amount(), e.currency()) + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forTransferReleased(FinancialWorkflowEvents.TransferReleasedEvent e) {
        return forQrCompleted(new FinancialWorkflowEvents.QrReleaseCompletedEvent(
                e.transactionId(),
                e.senderId(),
                e.receiverId(),
                e.payoutActorUserId(),
                e.releaseChannel()));
    }

    public List<FinancialNotificationDispatch> forWalletTopupRequested(FinancialWorkflowEvents.WalletTopupRequestedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "TOPUP-" + e.topupRequestId();
        out.add(dispatch(
                idem("topup", "req", "user", e.topupRequestId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.WALLET_TOPUP_REQUESTED_USER,
                "Top-up requested — " + ref,
                "Reference " + ref + ". Amount " + amountLine(e.amount(), e.currency()) + ". Visit branch #" + e.branchId() + " to complete cash-in.",
                InAppNotification.NotificationType.TRANSACTION));
        for (Long uid : distinctUserIds(branchCashiers(e.branchId()))) {
            out.add(dispatch(
                    idem("topup", "req", "cashier", e.topupRequestId(), uid),
                    uid,
                    NotificationTemplateKeys.WALLET_TOPUP_REQUESTED_CASHIER,
                    "Pending top-up — " + ref,
                    "Queue alert " + ref + ". User id " + e.userId() + ". Amount " + amountLine(e.amount(), e.currency()) + ".",
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forWalletTopupCompleted(FinancialWorkflowEvents.WalletTopupCompletedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "TOPUP-" + e.topupRequestId();
        out.add(dispatch(
                idem("topup", "done", "user", e.topupRequestId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.WALLET_TOPUP_COMPLETED_USER,
                "Top-up completed — " + ref,
                "Reference " + ref + ". Credited " + amountLine(e.amount(), e.currency())
                        + ". New balance (this currency) " + (e.newBalanceSameCurrency() != null ? e.newBalanceSameCurrency().toPlainString() : "n/a") + ".",
                InAppNotification.NotificationType.TRANSACTION));
        BigDecimal threshold = thresholds.getWalletTopupBranchManagerAlert();
        if (threshold != null && e.amount() != null && e.amount().compareTo(threshold) >= 0) {
            for (Long uid : distinctUserIds(branchManagers(e.branchId()))) {
                out.add(dispatch(
                        idem("topup", "large", e.topupRequestId(), uid),
                        uid,
                        NotificationTemplateKeys.WALLET_TOPUP_BRANCH_LARGE_CASH,
                        "Large cash-in — " + ref,
                        "Reference " + ref + ". Amount " + amountLine(e.amount(), e.currency()) + " at branch #" + e.branchId() + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forWalletWithdrawalRequested(FinancialWorkflowEvents.WalletWithdrawalRequestedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "CASHOUT-" + e.cashOutRequestId();
        String branchLine = branchLine(e.branchId());
        out.add(dispatch(
                idem("cashout", "req", "user", e.cashOutRequestId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.WALLET_WITHDRAW_REQUESTED_USER,
                "Cash-out requested — " + ref,
                "Reference " + ref + ". Amount " + amountLine(e.amount(), e.currency()) + ". " + branchLine,
                InAppNotification.NotificationType.TRANSACTION));
        for (Long uid : distinctUserIds(branchCashiers(e.branchId()))) {
            out.add(dispatch(
                    idem("cashout", "req", "cashier", e.cashOutRequestId(), uid),
                    uid,
                    NotificationTemplateKeys.WALLET_WITHDRAW_REQUESTED_CASHIER,
                    "Pending cash-out — " + ref,
                    "Queue " + ref + ". User id " + e.userId() + ". Amount " + amountLine(e.amount(), e.currency()) + ".",
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        for (Long uid : distinctUserIds(branchManagers(e.branchId()))) {
            out.add(dispatch(
                    idem("cashout", "req", "bm", e.cashOutRequestId(), uid),
                    uid,
                    NotificationTemplateKeys.WALLET_WITHDRAW_BRANCH_LIQUIDITY,
                    "Liquidity: cash-out pending — " + ref,
                    "Reference " + ref + ". Amount " + amountLine(e.amount(), e.currency()) + " may impact branch cash.",
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forWalletWithdrawalCompleted(FinancialWorkflowEvents.WalletWithdrawalCompletedEvent e) {
        String ref = "CASHOUT-" + e.cashOutRequestId();
        return List.of(dispatch(
                idem("cashout", "done", "user", e.cashOutRequestId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.WALLET_WITHDRAW_COMPLETED_USER,
                "Cash-out completed — " + ref,
                "Reference " + ref + ". Amount " + amountLine(e.amount(), e.currency()) + " paid at branch #" + e.branchId() + ".",
                InAppNotification.NotificationType.TRANSACTION));
    }

    public List<FinancialNotificationDispatch> forMerchantPayment(FinancialWorkflowEvents.MerchantPaymentCompletedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = e.paymentRef() != null ? e.paymentRef() : "MERCHTX-" + e.merchantTransactionId();
        out.add(dispatch(
                idem("merch", "pay", "payer", e.merchantTransactionId(), e.payerUserId()),
                e.payerUserId(),
                NotificationTemplateKeys.MERCHANT_PAYMENT_PAYER_RECEIPT,
                "Payment receipt — " + ref,
                "Paid " + amountLine(e.amount(), e.currency()) + " to " + nz(e.merchantDisplayName())
                        + ". Fee " + (e.platformFee() != null ? e.platformFee().toPlainString() : "0") + " " + nz(e.currency())
                        + ". Reference " + ref + ".",
                InAppNotification.NotificationType.TRANSACTION));
        out.add(dispatch(
                idem("merch", "pay", "owner", e.merchantTransactionId(), e.merchantOwnerUserId()),
                e.merchantOwnerUserId(),
                NotificationTemplateKeys.MERCHANT_PAYMENT_OWNER,
                "Payment received — " + ref,
                "Gross " + amountLine(e.amount(), e.currency()) + ", net " + (e.merchantNet() != null ? e.merchantNet().toPlainString() : "0")
                        + " " + nz(e.currency()) + ". Settlement status: pending standard cycle. Ref " + ref + ".",
                InAppNotification.NotificationType.TRANSACTION));
        if (e.highValuePerTradePlatformNotify()) {
            for (Long uid : platformOwners()) {
                out.add(dispatch(
                        idem("merch", "pay", "platform", e.merchantTransactionId(), uid),
                        uid,
                        NotificationTemplateKeys.MERCHANT_PAYMENT_PLATFORM_DIGEST,
                        "Merchant fee highlight — " + ref,
                        "Large card/wallet payment. Ref " + ref + ". Processing fee " + (e.platformFee() != null ? e.platformFee().toPlainString() : "0") + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forBillCreated(FinancialWorkflowEvents.BillPaymentCreatedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "BILL-" + e.billPaymentRequestId();
        out.add(dispatch(
                idem("bill", "created", "user", e.billPaymentRequestId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.BILL_USER_STATUS,
                "Bill payment processing — " + ref,
                "Reference " + ref + ". Provider " + nz(e.providerName()) + ". Amount " + amountLine(e.amount(), e.currency())
                        + ", fee " + (e.platformFee() != null ? e.platformFee().toPlainString() : "0") + ". Status PROCESSING.",
                InAppNotification.NotificationType.TRANSACTION));
        if (e.manualQueue()) {
            for (Long uid : distinctUserIds(allCashiers())) {
                out.add(dispatch(
                        idem("bill", "manual", "cashier", e.billPaymentRequestId(), uid),
                        uid,
                        NotificationTemplateKeys.BILL_MANUAL_CASHIER_QUEUE,
                        "Manual bill queue — " + ref,
                        "Reference " + ref + ". User id " + e.userId() + ". Amount " + amountLine(e.amount(), e.currency()) + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forBillCompleted(FinancialWorkflowEvents.BillPaymentCompletedEvent e) {
        String ref = "BILL-" + e.billPaymentRequestId();
        return List.of(dispatch(
                idem("bill", "done", "user", e.billPaymentRequestId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.BILL_USER_STATUS,
                "Bill payment update — " + ref,
                "Reference " + ref + ". Status " + nz(e.status()) + ". Amount " + amountLine(e.amount(), e.currency())
                        + ". Payment ref " + nz(e.paymentRef()) + ".",
                InAppNotification.NotificationType.TRANSACTION));
    }

    public List<FinancialNotificationDispatch> forBatchSubmitted(FinancialWorkflowEvents.BatchJobSubmittedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "BATCH-" + e.batchJobId();
        out.add(dispatch(
                idem("batch", "sub", "corp", e.batchJobId(), e.submitterUserId()),
                e.submitterUserId(),
                NotificationTemplateKeys.BATCH_SUBMITTED_CORPORATE,
                "Batch submitted — " + ref,
                "Reference " + ref + ". Rows " + e.rowCount() + ", estimated total USD " + e.totalAmountUsd()
                        + (e.pendingApproval() ? ". Pending approval." : "."),
                InAppNotification.NotificationType.TRANSACTION));
        for (Long uid : motherBranchAdmins()) {
            out.add(dispatch(
                    idem("batch", "sub", "mother", e.batchJobId(), uid),
                    uid,
                    NotificationTemplateKeys.BATCH_SUBMITTED_MOTHER_APPROVAL,
                    "Batch needs approval — " + ref,
                    "Corporate batch " + ref + " from user " + e.submitterUserId() + ". Rows " + e.rowCount() + ".",
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forBatchCompleted(FinancialWorkflowEvents.BatchJobCompletedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "BATCH-" + e.batchJobId();
        out.add(dispatch(
                idem("batch", "done", "corp", e.batchJobId(), e.submitterUserId()),
                e.submitterUserId(),
                NotificationTemplateKeys.BATCH_COMPLETED_REPORT,
                "Batch completed — " + ref,
                "Reference " + ref + ". Success " + e.successCount() + ", failed " + e.failedCount() + ".",
                InAppNotification.NotificationType.TRANSACTION));
        BigDecimal vol = thresholds.getBatchHighVolumeUsd();
        if (vol != null && e.successCount() + e.failedCount() > 0) {
            for (Long uid : platformOwners()) {
                out.add(dispatch(
                        idem("batch", "done", "platform", e.batchJobId(), uid),
                        uid,
                        NotificationTemplateKeys.BATCH_PLATFORM_HIGH_VOLUME,
                        "Batch volume notice — " + ref,
                        "Reference " + ref + ". Outcomes success=" + e.successCount() + " failed=" + e.failedCount() + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forLoanDue(FinancialWorkflowEvents.LoanPaymentDueEvent e) {
        return List.of(dispatch(
                idem("loan", "due", e.scheduleId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.LOAN_DUE_USER,
                "Loan instalment due",
                "Loan " + e.loanId() + ", schedule " + e.scheduleId() + ". Due " + nz(e.dueDate())
                        + ". Amount due " + amountLine(e.amountDue(), e.currency()) + ".",
                InAppNotification.NotificationType.ALERT));
    }

    public List<FinancialNotificationDispatch> forLoanPaymentFailed(FinancialWorkflowEvents.LoanPaymentFailedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        out.add(dispatch(
                idem("loan", "fail", e.scheduleId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.LOAN_PAYMENT_FAILED_USER,
                "Loan payment issue",
                "Loan " + e.loanId() + ", schedule " + e.scheduleId() + ". Reason " + nz(e.reasonCode())
                        + ". Late fee " + (e.lateFee() != null ? e.lateFee().toPlainString() : "0") + ".",
                InAppNotification.NotificationType.ALERT));
        if (e.escalatedDefault()) {
            for (Long uid : motherBranchAdmins()) {
                out.add(dispatch(
                        idem("loan", "default", e.loanId(), uid),
                        uid,
                        NotificationTemplateKeys.LOAN_DEFAULT_MOTHER,
                        "Loan default escalation",
                        "Loan " + e.loanId() + " user " + e.userId() + " escalated. Reason " + nz(e.reasonCode()) + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
            for (Long uid : platformOwners()) {
                out.add(dispatch(
                        idem("loan", "risk", e.loanId(), uid),
                        uid,
                        NotificationTemplateKeys.LOAN_RISK_PLATFORM_DIGEST,
                        "Portfolio risk digest item",
                        "Default escalation loan " + e.loanId() + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forEscrowFunded(FinancialWorkflowEvents.EscrowFundedEvent e) {
        return escrowPartyDispatches(e.contractId(), e.initiatorUserId(), e.beneficiaryUserId(), NotificationTemplateKeys.ESCROW_FUNDED_PARTY,
                "Escrow funded — contract " + e.contractId(),
                "Contract " + e.contractId() + " funded. Amount " + amountLine(e.amount(), e.currency())
                        + ". Condition " + nz(e.conditionType()) + ".");
    }

    public List<FinancialNotificationDispatch> forEscrowReleased(FinancialWorkflowEvents.EscrowReleasedEvent e) {
        return escrowPartyDispatches(e.contractId(), e.initiatorUserId(), e.beneficiaryUserId(), NotificationTemplateKeys.ESCROW_RELEASED_PARTY,
                "Escrow released — contract " + e.contractId(),
                "Contract " + e.contractId() + " released. Amount " + amountLine(e.amount(), e.currency()) + ".");
    }

    public List<FinancialNotificationDispatch> forEscrowDisputed(FinancialWorkflowEvents.EscrowDisputedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        out.addAll(escrowPartyDispatches(e.contractId(), e.initiatorUserId(), e.beneficiaryUserId(), NotificationTemplateKeys.ESCROW_DISPUTED_PARTY,
                "Escrow disputed — contract " + e.contractId(),
                "Contract " + e.contractId() + " disputed. Category " + nz(e.reasonCategory()) + "."));

        if ("CASHIER_WITNESS".equalsIgnoreCase(nz(e.conditionType()))) {
            for (Long uid : allCashiers()) {
                out.add(dispatch(
                        idem("escrow", "witness", e.contractId(), uid),
                        uid,
                        NotificationTemplateKeys.ESCROW_CASHIER_WITNESS,
                        "Escrow witness action",
                        "Contract " + e.contractId() + " requires cashier witness per policy.",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }
        for (Long uid : motherBranchAdmins()) {
            out.add(dispatch(
                    idem("escrow", "dispute_mother", e.contractId(), uid),
                    uid,
                    NotificationTemplateKeys.ESCROW_DISPUTE_MOTHER,
                    "Escrow dispute escalation",
                    "Contract " + e.contractId() + ". Category " + nz(e.reasonCategory()) + ".",
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        return out;
    }

    private List<FinancialNotificationDispatch> escrowPartyDispatches(Long contractId, Long initiator, Long beneficiary,
                                                                      String template, String title, String body) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        for (Long uid : distinctUserIds(List.of(initiator, beneficiary))) {
            if (uid == null) continue;
            out.add(dispatch(
                    idem("escrow", template, contractId, uid),
                    uid,
                    template,
                    title,
                    body,
                    InAppNotification.NotificationType.TRANSACTION,
                    true));
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forTradingPlaced(FinancialWorkflowEvents.TradingOrderPlacedEvent e) {
        return List.of(dispatch(
                idem("trade", "placed", e.orderId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.TRADING_ORDER_PLACED,
                "Order placed — " + e.orderId(),
                "Symbol " + nz(e.symbol()) + " side " + nz(e.side()) + " qty " + e.quantity()
                        + ". Indicative fee USD " + (e.indicatedFeeUsd() != null ? e.indicatedFeeUsd().toPlainString() : "0") + ".",
                InAppNotification.NotificationType.TRANSACTION));
    }

    public List<FinancialNotificationDispatch> forTradingFilled(FinancialWorkflowEvents.TradingOrderFilledEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        out.add(dispatch(
                idem("trade", "filled", e.orderId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.TRADING_ORDER_FILLED,
                "Order filled — " + e.orderId(),
                "Symbol " + nz(e.symbol()) + " filled at " + (e.fillPrice() != null ? e.fillPrice().toPlainString() : "n/a")
                        + ". Fee USD " + (e.feeUsd() != null ? e.feeUsd().toPlainString() : "0") + ".",
                InAppNotification.NotificationType.TRANSACTION));
        if (e.highValuePerTradePlatformNotify()) {
            for (Long uid : platformOwners()) {
                out.add(dispatch(
                        idem("trade", "platform", e.orderId(), uid),
                        uid,
                        NotificationTemplateKeys.TRADING_PLATFORM_DIGEST,
                        "Trading fee digest item",
                        "Large trade order " + e.orderId() + ". Fee USD " + (e.feeUsd() != null ? e.feeUsd().toPlainString() : "0") + ".",
                        InAppNotification.NotificationType.ALERT,
                        false));
            }
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forTradingFailed(FinancialWorkflowEvents.TradingOrderFailedEvent e) {
        if (e.userId() == null) return List.of();
        return List.of(dispatch(
                idem("trade", "failed", e.tradingAccountId(), e.userId(), Objects.hash(e.symbol(), e.reasonCode())),
                e.userId(),
                NotificationTemplateKeys.TRADING_ORDER_FAILED,
                "Order failed",
                "Trading account " + e.tradingAccountId() + ". Symbol " + nz(e.symbol()) + ". Reason " + nz(e.reasonCode()) + ".",
                InAppNotification.NotificationType.ALERT));
    }

    public List<FinancialNotificationDispatch> forAmlAlertCreated(FinancialWorkflowEvents.AmlAlertCreatedEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        String ref = "AML-" + e.alertId();
        for (Long uid : auditors()) {
            out.add(dispatch(
                    idem("aml", "auditor", e.alertId(), uid),
                    uid,
                    NotificationTemplateKeys.AML_ALERT_AUDITOR,
                    "AML alert — " + ref,
                    staffSummary(ref, e),
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        for (Long uid : motherBranchAdmins()) {
            out.add(dispatch(
                    idem("aml", "mother", e.alertId(), uid),
                    uid,
                    NotificationTemplateKeys.AML_ALERT_MOTHER,
                    "AML alert — " + ref,
                    staffSummary(ref, e),
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        for (Long uid : platformOwners()) {
            out.add(dispatch(
                    idem("aml", "platform", e.alertId(), uid),
                    uid,
                    NotificationTemplateKeys.AML_ALERT_PLATFORM_CRITICAL,
                    "AML critical — " + ref,
                    staffSummary(ref, e),
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        return out;
    }

    private static String staffSummary(String ref, FinancialWorkflowEvents.AmlAlertCreatedEvent e) {
        return ref + ". Severity " + nz(e.severity()) + ". Summary: " + nz(e.summaryForStaff()) + ". Subject user id " + e.subjectUserId() + ".";
    }

    public List<FinancialNotificationDispatch> forLiquidityLow(FinancialWorkflowEvents.LiquidityLowEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        for (Long uid : distinctUserIds(branchManagers(e.branchId()))) {
            out.add(dispatch(
                    idem("liq", "low", e.branchId(), e.currency(), uid),
                    uid,
                    NotificationTemplateKeys.LIQUIDITY_LOW_BRANCH,
                    "Low branch cash — " + nz(e.branchName()),
                    "Branch " + nz(e.branchName()) + " (" + e.branchId() + ") " + nz(e.currency())
                            + " available " + (e.available() != null ? e.available().toPlainString() : "0")
                            + ", threshold " + (e.lowThreshold() != null ? e.lowThreshold().toPlainString() : "0") + ".",
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forAccountFrozen(FinancialWorkflowEvents.AccountFrozenEvent e) {
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        out.add(dispatch(
                idem("freeze", "user", e.walletId(), e.userId()),
                e.userId(),
                NotificationTemplateKeys.ACCOUNT_FROZEN_USER,
                "Account restricted — case " + nz(e.caseId()),
                "Your wallet access is restricted. Case " + nz(e.caseId()) + ". Reason category: " + nz(e.customerVisibleReason())
                        + ". Appeals: " + nz(e.appealUrl()) + ". SLA: we respond within regulatory timelines.",
                InAppNotification.NotificationType.ALERT));
        for (Long uid : distinctUserIds(combine(auditors(), motherBranchAdmins()))) {
            out.add(dispatch(
                    idem("freeze", "internal", e.walletId(), uid),
                    uid,
                    NotificationTemplateKeys.ACCOUNT_FROZEN_INTERNAL,
                    "Wallet frozen (internal) — " + e.walletId(),
                    "Wallet " + e.walletId() + " user " + e.userId() + ". Case " + nz(e.caseId())
                            + ". Category " + nz(e.reasonCategory()) + ". (No AML narrative to customer.)",
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        return out;
    }

    public List<FinancialNotificationDispatch> forDisputeOpened(FinancialWorkflowEvents.DisputeOpenedEvent e) {
        return List.of(dispatch(
                idem("dispute", "open", e.disputeId(), e.reporterUserId()),
                e.reporterUserId(),
                NotificationTemplateKeys.DISPUTE_OPENED_REPORTER,
                "Dispute opened — DSP-" + e.disputeId(),
                "We received your dispute for transaction ref " + e.transactionId() + ". Category " + nz(e.category()) + ".",
                InAppNotification.NotificationType.ALERT));
    }

    public List<FinancialNotificationDispatch> forDisputeResolved(FinancialWorkflowEvents.DisputeResolvedEvent e) {
        return List.of(dispatch(
                idem("dispute", "resolved", e.disputeId(), e.reporterUserId()),
                e.reporterUserId(),
                NotificationTemplateKeys.DISPUTE_RESOLVED_REPORTER,
                "Dispute resolved — DSP-" + e.disputeId(),
                nz(e.resolutionSummary()),
                InAppNotification.NotificationType.ALERT));
    }

    // --- helpers ---

    private List<FinancialNotificationDispatch> senderBranchInternalAlerts(Transaction tx, Long senderBranchId, String ref,
                                                                         BigDecimal amount, String currency) {
        if (senderBranchId == null) return List.of();
        List<FinancialNotificationDispatch> out = new ArrayList<>();
        User receiver = tx.getReceiver();
        String receiverContact = receiver.getPhone() != null && !receiver.getPhone().isBlank()
                ? maskPhone(receiver.getPhone()) : maskEmail(receiver.getEmail());
        String msg = "New transfer " + ref + ". Receiver display " + receiver.getUsername()
                + ", contact " + receiverContact + ". Amount " + amountLine(amount, currency)
                + ". (No passcode in this alert.)";
        for (Long uid : distinctUserIds(branchManagers(senderBranchId))) {
            out.add(dispatch(
                    idem("transfer", "internal_sender_branch", tx.getId(), uid),
                    uid,
                    NotificationTemplateKeys.TRANSFER_CREATED_BRANCH_ACTION,
                    "Sender branch transfer alert — " + ref,
                    msg,
                    InAppNotification.NotificationType.ALERT,
                    false));
        }
        return out;
    }

    private static FinancialNotificationDispatch dispatch(String idemKey, Long recipientUserId, String templateKey,
                                                          String title, String body,
                                                          InAppNotification.NotificationType type) {
        return new FinancialNotificationDispatch(idemKey, recipientUserId, templateKey, title, body, type);
    }

    private static FinancialNotificationDispatch dispatch(String idemKey, Long recipientUserId, String templateKey,
                                                          String title, String body,
                                                          InAppNotification.NotificationType type,
                                                          boolean sendWhatsApp) {
        return new FinancialNotificationDispatch(idemKey, recipientUserId, templateKey, title, body, type, sendWhatsApp);
    }

    private static String idem(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (sb.length() > 0) sb.append(':');
            sb.append(p == null ? "null" : p.toString());
        }
        if (sb.length() > 512) {
            return sb.substring(0, 512);
        }
        return sb.toString();
    }

    private static String amountLine(BigDecimal amount, String currency) {
        return (amount != null ? amount.toPlainString() : "0") + " " + nz(currency);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String maskUserId(Long userId) {
        if (userId == null) return "receiver";
        return "user…" + (userId % 1000);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 2);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        return "***@" + email.substring(email.indexOf('@') + 1);
    }

    private String branchLine(Long branchId) {
        if (branchId == null) return "";
        return branchRepository.findById(branchId)
                .map(b -> " Branch: " + b.getName() + branchPhoneSegment(b))
                .orElse(" Branch id: #" + branchId + ".");
    }

    private String branchPhoneSegment(Branch b) {
        if (b.getPhone() == null || b.getPhone().isBlank()) return ".";
        return ", phone " + maskPhone(b.getPhone()) + ".";
    }

    private List<Long> branchCashiers(Long branchId) {
        if (branchId == null) return List.of();
        return userRepository.findByBranch_IdAndRole(branchId, UserRole.CASHIER).stream().map(User::getId).toList();
    }

    private List<Long> branchManagers(Long branchId) {
        if (branchId == null) return List.of();
        return userRepository.findByBranch_IdAndRole(branchId, UserRole.BRANCH_MANAGER).stream().map(User::getId).toList();
    }

    private List<Long> branchStaff(Long branchId) {
        List<Long> ids = new ArrayList<>();
        ids.addAll(branchCashiers(branchId));
        ids.addAll(branchManagers(branchId));
        return ids;
    }

    private List<Long> platformOwners() {
        return userRepository.findByRole(UserRole.PLATFORM_OWNER).stream().map(User::getId).toList();
    }

    private List<Long> motherBranchAdmins() {
        return userRepository.findByRole(UserRole.MOTHER_BRANCH_ADMIN).stream().map(User::getId).toList();
    }

    private List<Long> auditors() {
        return userRepository.findByRole(UserRole.AUDITOR).stream().map(User::getId).toList();
    }

    private List<Long> allCashiers() {
        return userRepository.findByRole(UserRole.CASHIER).stream().map(User::getId).toList();
    }

    private List<Long> platformOwnersAndMother() {
        List<Long> out = new ArrayList<>();
        out.addAll(platformOwners());
        out.addAll(motherBranchAdmins());
        return distinctUserIds(out);
    }

    private static List<Long> distinctUserIds(List<Long> ids) {
        Set<Long> set = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) set.add(id);
        }
        return new ArrayList<>(set);
    }

    private static List<Long> combine(List<Long> a, List<Long> b) {
        List<Long> out = new ArrayList<>(a);
        out.addAll(b);
        return distinctUserIds(out);
    }
}
