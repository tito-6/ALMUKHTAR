package com.mycompany.transfersystem.notification;

/**
 * Stable keys for in-app titles and dedupe grouping; WhatsApp templates may map 1:1 in Meta Business.
 */
public final class NotificationTemplateKeys {

    private NotificationTemplateKeys() {}

    public static final String TRANSFER_CREATED_SENDER = "transfer.created.sender";
    public static final String TRANSFER_CREATED_RECEIVER = "transfer.created.receiver";
    public static final String TRANSFER_CREATED_BRANCH_ACTION = "transfer.created.branch_action";
    public static final String TRANSFER_CREATED_PLATFORM_SUMMARY = "transfer.created.platform_summary";

    public static final String TRANSFER_COMPLETED_SENDER_RECEIPT = "transfer.completed.sender_receipt";
    public static final String TRANSFER_COMPLETED_RECEIVER_PAYOUT = "transfer.completed.receiver_payout";
    public static final String TRANSFER_COMPLETED_BRANCH_CASH = "transfer.completed.branch_cash";
    public static final String TRANSFER_COMPLETED_PLATFORM_SUMMARY = "transfer.completed.platform_summary";

    public static final String TRANSFER_FAILED_SENDER = "transfer.failed.sender";
    public static final String TRANSFER_CANCELLED_PARTIES = "transfer.cancelled.parties";

    public static final String QR_CREATED_SENDER = "qr.created.sender";
    public static final String QR_CREATED_RECEIVER = "qr.created.receiver";
    public static final String QR_COMPLETED_CASHIER = "qr.completed.cashier";
    public static final String QR_COMPLETED_SENDER = "qr.completed.sender";
    public static final String QR_COMPLETED_RECEIVER = "qr.completed.receiver";

    public static final String WALLET_TOPUP_REQUESTED_USER = "wallet.topup.requested.user";
    public static final String WALLET_TOPUP_REQUESTED_CASHIER = "wallet.topup.requested.cashier";
    public static final String WALLET_TOPUP_COMPLETED_USER = "wallet.topup.completed.user";
    public static final String WALLET_TOPUP_BRANCH_LARGE_CASH = "wallet.topup.branch_large_cash";

    public static final String WALLET_WITHDRAW_REQUESTED_USER = "wallet.withdraw.requested.user";
    public static final String WALLET_WITHDRAW_REQUESTED_CASHIER = "wallet.withdraw.requested.cashier";
    public static final String WALLET_WITHDRAW_BRANCH_LIQUIDITY = "wallet.withdraw.branch_liquidity";
    public static final String WALLET_WITHDRAW_COMPLETED_USER = "wallet.withdraw.completed.user";

    public static final String MERCHANT_PAYMENT_PAYER_RECEIPT = "merchant.payment.payer_receipt";
    public static final String MERCHANT_PAYMENT_OWNER = "merchant.payment.owner";
    public static final String MERCHANT_PAYMENT_PLATFORM_DIGEST = "merchant.payment.platform_digest";

    public static final String BILL_USER_STATUS = "bill.user.status";
    public static final String BILL_MANUAL_CASHIER_QUEUE = "bill.manual.cashier_queue";
    public static final String BILL_BRANCH_OVERDUE = "bill.branch.overdue_queue";

    public static final String BATCH_SUBMITTED_CORPORATE = "batch.submitted.corporate";
    public static final String BATCH_SUBMITTED_MOTHER_APPROVAL = "batch.submitted.mother_approval";
    public static final String BATCH_COMPLETED_REPORT = "batch.completed.report";
    public static final String BATCH_PLATFORM_HIGH_VOLUME = "batch.platform.high_volume";

    public static final String LOAN_DUE_USER = "loan.due.user";
    public static final String LOAN_PAYMENT_FAILED_USER = "loan.payment_failed.user";
    public static final String LOAN_DEFAULT_MOTHER = "loan.default.mother";
    public static final String LOAN_RISK_PLATFORM_DIGEST = "loan.risk.platform_digest";

    public static final String ESCROW_FUNDED_PARTY = "escrow.funded.party";
    public static final String ESCROW_RELEASED_PARTY = "escrow.released.party";
    public static final String ESCROW_DISPUTED_PARTY = "escrow.disputed.party";
    public static final String ESCROW_CASHIER_WITNESS = "escrow.cashier_witness";
    public static final String ESCROW_DISPUTE_MOTHER = "escrow.dispute.mother";

    public static final String TRADING_ORDER_PLACED = "trading.order.placed";
    public static final String TRADING_ORDER_FILLED = "trading.order.filled";
    public static final String TRADING_ORDER_FAILED = "trading.order.failed";
    public static final String TRADING_PLATFORM_DIGEST = "trading.platform.digest";

    public static final String AML_ALERT_AUDITOR = "aml.alert.auditor";
    public static final String AML_ALERT_MOTHER = "aml.alert.mother";
    public static final String AML_ALERT_PLATFORM_CRITICAL = "aml.alert.platform_critical";

    public static final String LIQUIDITY_LOW_BRANCH = "liquidity.low.branch";

    public static final String ACCOUNT_FROZEN_USER = "account.frozen.user";
    public static final String ACCOUNT_FROZEN_INTERNAL = "account.frozen.internal";

    public static final String DISPUTE_OPENED_REPORTER = "dispute.opened.reporter";
    public static final String DISPUTE_RESOLVED_REPORTER = "dispute.resolved.reporter";
}
