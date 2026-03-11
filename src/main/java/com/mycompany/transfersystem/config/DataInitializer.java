package com.mycompany.transfersystem.config;

import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.*;
import com.mycompany.transfersystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CommissionRateRepository commissionRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired(required = false)
    private com.mycompany.transfersystem.repository.TrustScoreRepository trustScoreRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create currencies if they don't exist
        if (currencyRepository.count() == 0) {
            createSampleCurrencies();
        }

        // Create branches if they don't exist
        if (branchRepository.count() == 0) {
            createSampleBranches();
        }

        // Create commission rates if they don't exist
        if (commissionRateRepository.count() == 0) {
            createSampleCommissionRates();
        }

        // Create sample users if they don't exist
        if (userRepository.count() == 0) {
            createSampleUsers();
        }

        // Create sample funds if they don't exist
        if (fundRepository.count() == 0) {
            createSampleFunds();
        }

        // Create trust scores for users who don't have one
        createTrustScoresForUsers();

        // Seed platform trading fees if applicable
        createPlatformTradingFeesIfNeeded();

        // Seed loan products for micro-lending (Phase III P1)
        createLoanProductsIfNeeded();

        // Seed bill providers (Phase III P2)
        createBillProvidersIfNeeded();
    }

    @Autowired(required = false)
    private com.mycompany.transfersystem.repository.PlatformTradingFeeRepository platformTradingFeeRepository;

    private void createPlatformTradingFeesIfNeeded() {
        if (platformTradingFeeRepository == null) return;
        if (platformTradingFeeRepository.count() > 0) return;
        java.time.Instant now = java.time.Instant.now();
        platformTradingFeeRepository.save(com.mycompany.transfersystem.entity.PlatformTradingFee.builder()
                .assetClass(com.mycompany.transfersystem.entity.enums.AssetClass.STOCK)
                .feeRateBps(10)
                .minFeeUsd(java.math.BigDecimal.valueOf(1))
                .maxFeeUsd(java.math.BigDecimal.valueOf(50))
                .effectiveFrom(now)
                .build());
        platformTradingFeeRepository.save(com.mycompany.transfersystem.entity.PlatformTradingFee.builder()
                .assetClass(com.mycompany.transfersystem.entity.enums.AssetClass.ETF)
                .feeRateBps(5)
                .minFeeUsd(java.math.BigDecimal.ZERO)
                .maxFeeUsd(java.math.BigDecimal.valueOf(25))
                .effectiveFrom(now)
                .build());
        platformTradingFeeRepository.save(com.mycompany.transfersystem.entity.PlatformTradingFee.builder()
                .assetClass(com.mycompany.transfersystem.entity.enums.AssetClass.FOREX)
                .feeRateBps(2)
                .minFeeUsd(java.math.BigDecimal.ZERO)
                .maxFeeUsd(null)
                .effectiveFrom(now)
                .build());
        platformTradingFeeRepository.save(com.mycompany.transfersystem.entity.PlatformTradingFee.builder()
                .assetClass(com.mycompany.transfersystem.entity.enums.AssetClass.CRYPTO)
                .feeRateBps(50)
                .minFeeUsd(java.math.BigDecimal.valueOf(0.5))
                .maxFeeUsd(java.math.BigDecimal.valueOf(100))
                .effectiveFrom(now)
                .build());
        System.out.println("Platform trading fees seeded.");
    }

    private void createTrustScoresForUsers() {
        if (trustScoreRepository == null) return;
        userRepository.findAll().forEach(user -> {
            if (trustScoreRepository.findByUser_Id(user.getId()).isEmpty()) {
                com.mycompany.transfersystem.entity.TrustScore ts = com.mycompany.transfersystem.entity.TrustScore.builder()
                        .user(user)
                        .score(0)
                        .tier("BRONZE")
                        .feeDiscountPct(java.math.BigDecimal.ZERO)
                        .updatedAt(java.time.Instant.now())
                        .build();
                trustScoreRepository.save(ts);
            }
        });
    }

    private void createSampleUsers() {
        // SYSTEM user for audit logs when no user context (e.g. system errors)
        if (!userRepository.existsByUsername("SYSTEM")) {
            User systemUser = new User();
            systemUser.setUsername("SYSTEM");
            systemUser.setPassword(passwordEncoder.encode("system-internal-no-login"));
            systemUser.setRole(UserRole.PLATFORM_OWNER);
            userRepository.save(systemUser);
        }

        // Platform Owner (software author; receives platform revenue)
        User platformOwner = new User();
        platformOwner.setUsername("admin");
        platformOwner.setPassword(passwordEncoder.encode("admin123"));
        platformOwner.setRole(UserRole.PLATFORM_OWNER);
        userRepository.save(platformOwner);

        // Mother Branch Admin (main branch; can create corporations and onboard branches)
        if (!userRepository.existsByUsername("mother_admin")) {
            User motherAdmin = new User();
            motherAdmin.setUsername("mother_admin");
            motherAdmin.setPassword(passwordEncoder.encode("mother123"));
            motherAdmin.setRole(UserRole.MOTHER_BRANCH_ADMIN);
            Branch mainBranch = branchRepository.findFirstByName("MAIN_ADMIN_BRANCH").orElse(null);
            if (mainBranch != null) motherAdmin.setBranch(mainBranch);
            userRepository.save(motherAdmin);
        }

        // Branch Manager
        User manager = new User();
        manager.setUsername("manager");
        manager.setPassword(passwordEncoder.encode("manager123"));
        manager.setRole(UserRole.BRANCH_MANAGER);
        userRepository.save(manager);

        // Cashier
        User cashier = new User();
        cashier.setUsername("cashier");
        cashier.setPassword(passwordEncoder.encode("cashier123"));
        cashier.setRole(UserRole.CASHIER);
        userRepository.save(cashier);

        // Auditor
        User auditor = new User();
        auditor.setUsername("auditor");
        auditor.setPassword(passwordEncoder.encode("auditor123"));
        auditor.setRole(UserRole.AUDITOR);
        userRepository.save(auditor);

        System.out.println("Sample users created:");
        System.out.println("- admin/admin123 (PLATFORM_OWNER)");
        System.out.println("- mother_admin/mother123 (MOTHER_BRANCH_ADMIN)");
        System.out.println("- manager/manager123 (BRANCH_MANAGER)");
        System.out.println("- cashier/cashier123 (CASHIER)");
        System.out.println("- auditor/auditor123 (AUDITOR)");
    }

    private void createSampleFunds() {
        // General Fund
        Fund generalFund = new Fund();
        generalFund.setName("General Fund");
        generalFund.setBalance(new BigDecimal("100000.00"));
        generalFund.setStatus(FundStatus.ACTIVE);
        fundRepository.save(generalFund);

        // Emergency Fund
        Fund emergencyFund = new Fund();
        emergencyFund.setName("Emergency Fund");
        emergencyFund.setBalance(new BigDecimal("50000.00"));
        emergencyFund.setStatus(FundStatus.ACTIVE);
        fundRepository.save(emergencyFund);

        // Inactive Fund
        Fund inactiveFund = new Fund();
        inactiveFund.setName("Inactive Fund");
        inactiveFund.setBalance(new BigDecimal("25000.00"));
        inactiveFund.setStatus(FundStatus.INACTIVE);
        fundRepository.save(inactiveFund);

        System.out.println("Sample funds created:");
        System.out.println("- General Fund ($100,000 - ACTIVE)");
        System.out.println("- Emergency Fund ($50,000 - ACTIVE)");
        System.out.println("- Inactive Fund ($25,000 - INACTIVE)");
    }

    private void createSampleCurrencies() {
        // USD
        Currency usd = new Currency("USD", "US Dollar", BigDecimal.ONE, "$", false, "DEFAULT");
        currencyRepository.save(usd);

        // EUR
        Currency eur = new Currency("EUR", "Euro", new BigDecimal("1.08"), "€", false, "DEFAULT");
        currencyRepository.save(eur);

        // GBP
        Currency gbp = new Currency("GBP", "British Pound", new BigDecimal("1.25"), "£", false, "DEFAULT");
        currencyRepository.save(gbp);

        // TL (Turkish Lira) - 1 USD = 30 TL
        Currency tl = new Currency("TL", "Turkish Lira", new BigDecimal("0.033"), "₺", false, "DEFAULT");
        currencyRepository.save(tl);

        System.out.println("Sample currencies created:");
        System.out.println("- USD (1.00)");
        System.out.println("- EUR (1.08)");
        System.out.println("- GBP (1.25)");
        System.out.println("- TL (0.033)");
    }

    private void createSampleBranches() {
        // Main Admin Branch
        Branch mainAdminBranch = new Branch();
        mainAdminBranch.setName("MAIN_ADMIN_BRANCH");
        branchRepository.save(mainAdminBranch);

        // Branch A
        Branch branchA = new Branch();
        branchA.setName("BRANCH_A");
        branchRepository.save(branchA);

        // Branch B
        Branch branchB = new Branch();
        branchB.setName("BRANCH_B");
        branchRepository.save(branchB);

        System.out.println("Sample branches created:");
        System.out.println("- MAIN_ADMIN_BRANCH");
        System.out.println("- BRANCH_A");
        System.out.println("- BRANCH_B");
    }

    private void createSampleCommissionRates() {
        // Get branches
        Branch mainAdminBranch = branchRepository.findFirstByName("MAIN_ADMIN_BRANCH").orElseThrow();
        Branch branchA = branchRepository.findFirstByName("BRANCH_A").orElseThrow();
        Branch branchB = branchRepository.findFirstByName("BRANCH_B").orElseThrow();

        // Platform Fees - linked to MAIN_ADMIN_BRANCH
        CommissionRate platformBaseFee = new CommissionRate(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE, new BigDecimal("1.50"));
        commissionRateRepository.save(platformBaseFee);

        CommissionRate platformExchangeProfit = new CommissionRate(mainAdminBranch, CommissionScope.PLATFORM_EXCHANGE_PROFIT, new BigDecimal("1.50"));
        commissionRateRepository.save(platformExchangeProfit);

        CommissionRate walletExchangeFee = new CommissionRate(mainAdminBranch, CommissionScope.WALLET_EXCHANGE, new BigDecimal("0.50"));
        commissionRateRepository.save(walletExchangeFee);

        // Branch A Fees
        CommissionRate branchASendingFee = new CommissionRate(branchA, CommissionScope.SENDING_BRANCH_FEE, new BigDecimal("1.50"));
        commissionRateRepository.save(branchASendingFee);

        CommissionRate branchAReceivingFee = new CommissionRate(branchA, CommissionScope.RECEIVING_BRANCH_FEE, new BigDecimal("4.00"));
        commissionRateRepository.save(branchAReceivingFee);

        // Branch B Fees
        CommissionRate branchBSendingFee = new CommissionRate(branchB, CommissionScope.SENDING_BRANCH_FEE, new BigDecimal("1.50"));
        commissionRateRepository.save(branchBSendingFee);

        CommissionRate branchBReceivingFee = new CommissionRate(branchB, CommissionScope.RECEIVING_BRANCH_FEE, new BigDecimal("4.00"));
        commissionRateRepository.save(branchBReceivingFee);

        System.out.println("Sample commission rates created:");
        System.out.println("- Platform Base Fee: $1.50/1000 USD (MAIN_ADMIN_BRANCH)");
        System.out.println("- Platform Exchange Profit: $1.50/1000 USD (MAIN_ADMIN_BRANCH)");
        System.out.println("- Wallet Exchange Fee: 0.50% (MAIN_ADMIN_BRANCH)");
        System.out.println("- Branch A Sending Fee: $1.50/1000 USD");
        System.out.println("- Branch A Receiving Fee: $4.00/1000 USD");
        System.out.println("- Branch B Sending Fee: $1.50/1000 USD");
        System.out.println("- Branch B Receiving Fee: $4.00/1000 USD");
    }

    @Autowired(required = false)
    private com.mycompany.transfersystem.repository.LoanProductRepository loanProductRepository;

    private void createLoanProductsIfNeeded() {
        if (loanProductRepository == null || loanProductRepository.count() > 0) return;
        loanProductRepository.save(com.mycompany.transfersystem.entity.LoanProduct.builder()
                .name("Tier A Standard")
                .minAmount(new BigDecimal("100"))
                .maxAmount(new BigDecimal("5000"))
                .minTermDays(30)
                .maxTermDays(365)
                .aprRate(new BigDecimal("18"))
                .originationFeePct(new BigDecimal("1.5"))
                .lateFeeAmount(new BigDecimal("25"))
                .riskTier("TIER_A")
                .active(true)
                .build());
        loanProductRepository.save(com.mycompany.transfersystem.entity.LoanProduct.builder()
                .name("Tier B Standard")
                .minAmount(new BigDecimal("50"))
                .maxAmount(new BigDecimal("2000"))
                .minTermDays(30)
                .maxTermDays(180)
                .aprRate(new BigDecimal("24"))
                .originationFeePct(new BigDecimal("2"))
                .lateFeeAmount(new BigDecimal("25"))
                .riskTier("TIER_B")
                .active(true)
                .build());
        loanProductRepository.save(com.mycompany.transfersystem.entity.LoanProduct.builder()
                .name("Tier C Standard")
                .minAmount(new BigDecimal("25"))
                .maxAmount(new BigDecimal("500"))
                .minTermDays(30)
                .maxTermDays(90)
                .aprRate(new BigDecimal("36"))
                .originationFeePct(new BigDecimal("3"))
                .lateFeeAmount(new BigDecimal("25"))
                .riskTier("TIER_C")
                .active(true)
                .build());
    }

    @Autowired(required = false)
    private com.mycompany.transfersystem.repository.BillProviderRepository billProviderRepository;

    private void createBillProvidersIfNeeded() {
        if (billProviderRepository == null || billProviderRepository.count() > 0) return;
        billProviderRepository.save(com.mycompany.transfersystem.entity.BillProvider.builder()
                .name("Syriatel Top-Up")
                .category("MOBILE_TOPUP")
                .country("SY")
                .apiIntegration("MANUAL")
                .processingFeePct(new BigDecimal("2"))
                .active(true)
                .build());
        billProviderRepository.save(com.mycompany.transfersystem.entity.BillProvider.builder()
                .name("MTN Syria Top-Up")
                .category("MOBILE_TOPUP")
                .country("SY")
                .apiIntegration("MANUAL")
                .processingFeePct(new BigDecimal("2"))
                .active(true)
                .build());
        billProviderRepository.save(com.mycompany.transfersystem.entity.BillProvider.builder()
                .name("Electricity Bill")
                .category("ELECTRICITY")
                .country("SY")
                .apiIntegration("MANUAL")
                .processingFeePct(new BigDecimal("1.5"))
                .active(true)
                .build());
        billProviderRepository.save(com.mycompany.transfersystem.entity.BillProvider.builder()
                .name("Water Bill")
                .category("WATER")
                .country("SY")
                .apiIntegration("MANUAL")
                .processingFeePct(new BigDecimal("1.5"))
                .active(true)
                .build());
        billProviderRepository.save(com.mycompany.transfersystem.entity.BillProvider.builder()
                .name("Internet Provider")
                .category("INTERNET")
                .country("SY")
                .apiIntegration("MANUAL")
                .processingFeePct(new BigDecimal("2"))
                .active(true)
                .build());
    }
}