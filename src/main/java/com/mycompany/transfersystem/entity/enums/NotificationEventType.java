package com.mycompany.transfersystem.entity.enums;

/**
 * Domain notification kinds mapped to WhatsApp templates in {@code NotificationDispatchService}.
 */
public enum NotificationEventType {

    transfer_sender_created(NotificationMessageCategory.OPERATIONAL, true, false),
    transfer_receiver_pickup_ready(NotificationMessageCategory.OPERATIONAL, true, false),
    transfer_sender_completed(NotificationMessageCategory.OPERATIONAL, true, false),
    transfer_receiver_completed(NotificationMessageCategory.OPERATIONAL, true, false),
    transfer_cancelled(NotificationMessageCategory.OPERATIONAL, true, false),
    transfer_failed(NotificationMessageCategory.OPERATIONAL, true, false),

    qr_release_code_created(NotificationMessageCategory.OPERATIONAL, true, false),
    qr_release_completed(NotificationMessageCategory.OPERATIONAL, true, false),

    wallet_topup_requested(NotificationMessageCategory.OPERATIONAL, true, false),
    wallet_topup_completed(NotificationMessageCategory.OPERATIONAL, true, false),
    wallet_withdrawal_ready(NotificationMessageCategory.OPERATIONAL, true, false),

    merchant_payment_payer_receipt(NotificationMessageCategory.OPERATIONAL, true, false),
    merchant_payment_merchant_receipt(NotificationMessageCategory.OPERATIONAL, true, false),

    bill_payment_processing(NotificationMessageCategory.OPERATIONAL, true, false),
    bill_payment_completed(NotificationMessageCategory.OPERATIONAL, true, false),

    batch_job_submitted(NotificationMessageCategory.OPERATIONAL, true, false),
    batch_job_completed(NotificationMessageCategory.OPERATIONAL, true, false),

    loan_due_reminder(NotificationMessageCategory.OPERATIONAL, true, false),
    loan_payment_success(NotificationMessageCategory.OPERATIONAL, true, false),
    loan_payment_failed(NotificationMessageCategory.OPERATIONAL, true, false),

    escrow_funded(NotificationMessageCategory.OPERATIONAL, true, false),
    escrow_released(NotificationMessageCategory.OPERATIONAL, true, false),
    escrow_disputed(NotificationMessageCategory.OPERATIONAL, false, false),

    trading_order_placed(NotificationMessageCategory.OPERATIONAL, true, false),
    trading_order_filled(NotificationMessageCategory.OPERATIONAL, true, false),
    trading_order_failed(NotificationMessageCategory.OPERATIONAL, true, false),

    suspicious_activity_internal_alert(NotificationMessageCategory.OPERATIONAL, false, false),
    liquidity_low_branch_alert(NotificationMessageCategory.OPERATIONAL, false, false),
    account_frozen_user_notice(NotificationMessageCategory.OPERATIONAL, false, false),

    dispute_opened(NotificationMessageCategory.OPERATIONAL, false, false),
    dispute_resolved(NotificationMessageCategory.OPERATIONAL, true, false),

    system_maintenance(NotificationMessageCategory.MARKETING, false, true);

    private final NotificationMessageCategory messageCategory;
    /** When true, {@code transactionAlertsEnabled=false} may skip delivery (non-security traffic). */
    private final boolean respectsTransactionAlertToggle;
    /** When true, {@code marketingEnabled=false} skips delivery. */
    private final boolean respectsMarketingToggle;

    NotificationEventType(NotificationMessageCategory messageCategory,
                          boolean respectsTransactionAlertToggle,
                          boolean respectsMarketingToggle) {
        this.messageCategory = messageCategory;
        this.respectsTransactionAlertToggle = respectsTransactionAlertToggle;
        this.respectsMarketingToggle = respectsMarketingToggle;
    }

    public NotificationMessageCategory getMessageCategory() {
        return messageCategory;
    }

    public boolean respectsTransactionAlertToggle() {
        return respectsTransactionAlertToggle;
    }

    public boolean respectsMarketingToggle() {
        return respectsMarketingToggle;
    }
}
