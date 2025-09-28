package com.mycompany.transfersystem.entity.enums;

public enum CommissionScope {
    PLATFORM_BASE_FEE,           // Mandatory $1.50/1000 USD fee
    PLATFORM_EXCHANGE_PROFIT,    // Conditional $1.50/1000 USD fee on non-matching currencies
    SENDING_BRANCH_FEE,          // Branch's configurable sending fee
    RECEIVING_BRANCH_FEE         // Branch's configurable receiving fee
}
