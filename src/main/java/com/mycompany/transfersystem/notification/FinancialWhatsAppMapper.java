package com.mycompany.transfersystem.notification;

import com.mycompany.transfersystem.entity.enums.NotificationEventType;

/**
 * Maps financial workflow template keys to {@link NotificationEventType} for preference rules and delivery logs.
 */
public final class FinancialWhatsAppMapper {

    private FinancialWhatsAppMapper() {
    }

    public static NotificationEventType eventTypeForTemplateKey(String templateKey) {
        if (templateKey == null) {
            return NotificationEventType.transfer_sender_created;
        }
        return switch (templateKey) {
            case NotificationTemplateKeys.TRANSFER_CREATED_SENDER -> NotificationEventType.transfer_sender_created;
            case NotificationTemplateKeys.TRANSFER_CREATED_RECEIVER -> NotificationEventType.transfer_receiver_pickup_ready;
            case NotificationTemplateKeys.TRANSFER_COMPLETED_SENDER_RECEIPT -> NotificationEventType.transfer_sender_completed;
            case NotificationTemplateKeys.TRANSFER_COMPLETED_RECEIVER_PAYOUT -> NotificationEventType.transfer_receiver_completed;
            case NotificationTemplateKeys.TRANSFER_FAILED_SENDER -> NotificationEventType.transfer_failed;
            case NotificationTemplateKeys.TRANSFER_CANCELLED_PARTIES -> NotificationEventType.transfer_cancelled;
            case NotificationTemplateKeys.QR_CREATED_SENDER, NotificationTemplateKeys.QR_CREATED_RECEIVER ->
                    NotificationEventType.qr_release_code_created;
            case NotificationTemplateKeys.QR_COMPLETED_SENDER, NotificationTemplateKeys.QR_COMPLETED_RECEIVER ->
                    NotificationEventType.qr_release_completed;
            case NotificationTemplateKeys.WALLET_TOPUP_REQUESTED_USER, NotificationTemplateKeys.WALLET_TOPUP_REQUESTED_CASHIER ->
                    NotificationEventType.wallet_topup_requested;
            case NotificationTemplateKeys.WALLET_TOPUP_COMPLETED_USER -> NotificationEventType.wallet_topup_completed;
            case NotificationTemplateKeys.WALLET_WITHDRAW_COMPLETED_USER -> NotificationEventType.wallet_withdrawal_ready;
            case NotificationTemplateKeys.MERCHANT_PAYMENT_PAYER_RECEIPT -> NotificationEventType.merchant_payment_payer_receipt;
            case NotificationTemplateKeys.MERCHANT_PAYMENT_OWNER -> NotificationEventType.merchant_payment_merchant_receipt;
            case NotificationTemplateKeys.BILL_USER_STATUS -> NotificationEventType.bill_payment_processing;
            case NotificationTemplateKeys.BATCH_SUBMITTED_CORPORATE, NotificationTemplateKeys.BATCH_SUBMITTED_MOTHER_APPROVAL ->
                    NotificationEventType.batch_job_submitted;
            case NotificationTemplateKeys.BATCH_COMPLETED_REPORT -> NotificationEventType.batch_job_completed;
            case NotificationTemplateKeys.LOAN_DUE_USER -> NotificationEventType.loan_due_reminder;
            case NotificationTemplateKeys.LOAN_PAYMENT_FAILED_USER -> NotificationEventType.loan_payment_failed;
            case NotificationTemplateKeys.ESCROW_FUNDED_PARTY -> NotificationEventType.escrow_funded;
            case NotificationTemplateKeys.ESCROW_RELEASED_PARTY -> NotificationEventType.escrow_released;
            case NotificationTemplateKeys.ESCROW_DISPUTED_PARTY -> NotificationEventType.escrow_disputed;
            case NotificationTemplateKeys.TRADING_ORDER_PLACED -> NotificationEventType.trading_order_placed;
            case NotificationTemplateKeys.TRADING_ORDER_FILLED -> NotificationEventType.trading_order_filled;
            case NotificationTemplateKeys.TRADING_ORDER_FAILED -> NotificationEventType.trading_order_failed;
            case "trading.price.alert" -> NotificationEventType.trading_order_placed;
            case NotificationTemplateKeys.AML_ALERT_AUDITOR, NotificationTemplateKeys.AML_ALERT_MOTHER,
                 NotificationTemplateKeys.AML_ALERT_PLATFORM_CRITICAL -> NotificationEventType.suspicious_activity_internal_alert;
            case NotificationTemplateKeys.LIQUIDITY_LOW_BRANCH -> NotificationEventType.liquidity_low_branch_alert;
            case NotificationTemplateKeys.ACCOUNT_FROZEN_USER -> NotificationEventType.account_frozen_user_notice;
            case NotificationTemplateKeys.DISPUTE_OPENED_REPORTER -> NotificationEventType.dispute_opened;
            case NotificationTemplateKeys.DISPUTE_RESOLVED_REPORTER -> NotificationEventType.dispute_resolved;
            case "internal.branch.ops" -> NotificationEventType.liquidity_low_branch_alert;
            default -> defaultForKey(templateKey);
        };
    }

    private static NotificationEventType defaultForKey(String templateKey) {
        if (templateKey.startsWith("transfer.")) {
            return NotificationEventType.transfer_sender_created;
        }
        if (templateKey.startsWith("wallet.")) {
            return NotificationEventType.wallet_topup_completed;
        }
        if (templateKey.startsWith("batch.")) {
            return NotificationEventType.batch_job_submitted;
        }
        if (templateKey.startsWith("bill.")) {
            return NotificationEventType.bill_payment_completed;
        }
        if (templateKey.startsWith("escrow.")) {
            return NotificationEventType.escrow_funded;
        }
        if (templateKey.startsWith("trading.")) {
            return NotificationEventType.trading_order_placed;
        }
        return NotificationEventType.transfer_sender_created;
    }
}
