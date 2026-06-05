package com.mycompany.transfersystem.entity.enums;

/**
 * AI helper persona. Mapped from {@link UserRole} unless the client requests {@link #TRADING_ASSISTANT}.
 */
public enum AiAgentRole {
    CUSTOMER_HELPER,
    CASHIER_COPILOT,
    BRANCH_MANAGER_COPILOT,
    PLATFORM_OWNER_COPILOT,
    TRADING_ASSISTANT
}
