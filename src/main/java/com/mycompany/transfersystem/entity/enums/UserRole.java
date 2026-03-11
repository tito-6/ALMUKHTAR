package com.mycompany.transfersystem.entity.enums;

public enum UserRole {
    /** Top-level system role (software author). Receives platform revenue. Replaces SUPER_ADMIN. */
    PLATFORM_OWNER,
    /** Runs mother/main branch; can create corporations and onboard new branches. */
    MOTHER_BRANCH_ADMIN,
    /** @deprecated Use PLATFORM_OWNER. Kept for backward compatibility. */
    @Deprecated
    SUPER_ADMIN,
    BRANCH_MANAGER,
    CASHIER,
    /** Manages corporate account, sub-accounts, payroll. */
    CORPORATE_ADMIN,
    /** End-user with digital wallet, trading account. */
    INDIVIDUAL_USER,
    AUDITOR
}