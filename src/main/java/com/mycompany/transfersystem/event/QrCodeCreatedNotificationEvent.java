package com.mycompany.transfersystem.event;

import java.time.Instant;

/**
 * Published when a pickup QR is generated for a pending transaction; WhatsApp dispatch runs after commit.
 */
public record QrCodeCreatedNotificationEvent(long transactionId, Instant expiresAt) {}
