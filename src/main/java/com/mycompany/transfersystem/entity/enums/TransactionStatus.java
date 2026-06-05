package com.mycompany.transfersystem.entity.enums;

/**
 * Transfer / money-movement lifecycle. Existing persisted values remain valid.
 */
public enum TransactionStatus {
    DRAFT,
    PENDING,
    /** Funds reserved; payout instructions prepared (e.g. comprehensive transfer). */
    PENDING_PAYOUT,
    /** Receiver may collect with passcode/QR at branch. */
    READY_FOR_PICKUP,
    COMPLETED,
    FAILED,
    /** Cash / QR / passcode payout completed at branch. */
    RELEASED,
    CANCELLED,
    EXPIRED
}