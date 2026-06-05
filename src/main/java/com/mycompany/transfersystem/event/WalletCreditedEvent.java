package com.mycompany.transfersystem.event;

import java.math.BigDecimal;

/**
 * Published after a successful wallet {@code credit} (balance increase).
 */
public record WalletCreditedEvent(
        Long walletId,
        Long userId,
        String currency,
        BigDecimal amount,
        String referenceId
) {}
