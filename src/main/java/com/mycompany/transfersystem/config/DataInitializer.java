package com.mycompany.transfersystem.config;

import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.*;
import com.mycompany.transfersystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @Autowired
    private TranslationRepository translationRepository;

    @Autowired
    private BranchCashInventoryRepository branchCashInventoryRepository;

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

        // Seed clean Syrian Arabic UI copy for backend-served localization bundles
        createSyrianArabicTranslationsIfNeeded();

        // Seed operational cash inventory for Syria cash-pickup flows
        createBranchCashInventoriesIfNeeded();

        createTradableAssetsIfNeeded();
    }

    @Autowired(required = false)
    private com.mycompany.transfersystem.repository.PlatformTradingFeeRepository platformTradingFeeRepository;

    @Autowired(required = false)
    private com.mycompany.transfersystem.repository.TradableAssetRepository tradableAssetRepository;

    private void createTradableAssetsIfNeeded() {
        if (tradableAssetRepository == null || tradableAssetRepository.count() > 0) {
            return;
        }
        java.util.List<com.mycompany.transfersystem.entity.TradableAsset> seed = java.util.List.of(
                com.mycompany.transfersystem.entity.TradableAsset.builder()
                        .symbol("AAPL").displayName("Apple Inc.").assetClass(com.mycompany.transfersystem.entity.enums.AssetClass.STOCK)
                        .enabled(true).currency("USD").market("US")
                        .minOrderValue(java.math.BigDecimal.ONE)
                        .maxOrderValue(new java.math.BigDecimal("500000"))
                        .riskLevel(com.mycompany.transfersystem.entity.enums.TradableAssetRiskLevel.MEDIUM)
                        .build(),
                com.mycompany.transfersystem.entity.TradableAsset.builder()
                        .symbol("MSFT").displayName("Microsoft Corp.").assetClass(com.mycompany.transfersystem.entity.enums.AssetClass.STOCK)
                        .enabled(true).currency("USD").market("US")
                        .minOrderValue(java.math.BigDecimal.ONE)
                        .maxOrderValue(new java.math.BigDecimal("500000"))
                        .riskLevel(com.mycompany.transfersystem.entity.enums.TradableAssetRiskLevel.MEDIUM)
                        .build(),
                com.mycompany.transfersystem.entity.TradableAsset.builder()
                        .symbol("EURUSD=X").displayName("EUR/USD").assetClass(com.mycompany.transfersystem.entity.enums.AssetClass.FOREX)
                        .enabled(true).currency("USD").market("FX")
                        .minOrderValue(new java.math.BigDecimal("100"))
                        .maxOrderValue(new java.math.BigDecimal("2000000"))
                        .riskLevel(com.mycompany.transfersystem.entity.enums.TradableAssetRiskLevel.HIGH)
                        .build()
        );
        tradableAssetRepository.saveAll(seed);
        System.out.println("Tradable assets seeded for sandbox trading.");
    }

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

        if (!userRepository.existsByUsername("corp_trader")) {
            User corp = new User();
            corp.setUsername("corp_trader");
            corp.setPassword(passwordEncoder.encode("corp123"));
            corp.setRole(UserRole.CORPORATE_ADMIN);
            corp.setPhone("+963930000001");
            userRepository.save(corp);
        }

        System.out.println("Sample users created:");
        System.out.println("- admin/admin123 (PLATFORM_OWNER)");
        System.out.println("- mother_admin/mother123 (MOTHER_BRANCH_ADMIN)");
        System.out.println("- manager/manager123 (BRANCH_MANAGER)");
        System.out.println("- cashier/cashier123 (CASHIER)");
        System.out.println("- auditor/auditor123 (AUDITOR)");
        System.out.println("- corp_trader/corp123 (CORPORATE_ADMIN)");
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

    private void createSyrianArabicTranslationsIfNeeded() {
        Map<String, String> english = new LinkedHashMap<>();
        english.put("app.name", "ALMUKHTAR Elite");
        english.put("common.language", "Language");
        english.put("common.direction", "ltr");
        english.put("common.continue", "Continue");
        english.put("common.cancel", "Cancel");
        english.put("common.confirm", "Confirm");
        english.put("common.save", "Save");
        english.put("common.close", "Close");
        english.put("common.search", "Search");
        english.put("common.amount", "Amount");
        english.put("common.currency", "Currency");
        english.put("common.status", "Status");
        english.put("common.branch", "Branch");
        english.put("common.fees", "Fees");
        english.put("common.total", "Total");
        english.put("auth.login.title", "Sign in");
        english.put("auth.username", "Username");
        english.put("auth.password", "Password");
        english.put("auth.login.submit", "Sign in");
        english.put("auth.error.invalid", "Username or password is incorrect.");
        english.put("home.balance", "Wallet balance");
        english.put("home.quickTransfer", "Send hawala");
        english.put("home.scanQr", "Scan QR");
        english.put("home.payBill", "Pay a bill");
        english.put("home.trade", "Trading");
        english.put("transfer.title", "Send money");
        english.put("transfer.senderBranch", "Sender branch");
        english.put("transfer.receiverBranch", "Receiver branch");
        english.put("transfer.receiverName", "Receiver name");
        english.put("transfer.receiverPhone", "Receiver phone");
        english.put("transfer.releaseCode", "Release code");
        english.put("transfer.cleanPayout", "Receiver gets the full amount. Fees are paid by the sender branch.");
        english.put("transfer.pending", "Transfer is pending until the release code is verified.");
        english.put("transfer.completed", "Transfer completed successfully.");
        english.put("transfer.failed", "Transfer failed. Please try again or contact the branch.");
        english.put("qr.title", "Smart-Bridge QR");
        english.put("qr.instructions", "Show this QR to the cashier at any ALMUKHTAR branch.");
        english.put("qr.expired", "This QR code has expired. Please request a new one.");
        english.put("qr.used", "This QR code was already used.");
        english.put("wallet.title", "My wallet");
        english.put("wallet.topup", "Top up wallet");
        english.put("wallet.exchange", "Exchange currency");
        english.put("wallet.frozen", "Wallet is temporarily frozen. Contact support.");
        english.put("kyc.title", "Identity verification");
        english.put("kyc.uploadId", "Upload ID document");
        english.put("kyc.selfie", "Upload selfie");
        english.put("kyc.pending", "Your verification is under review.");
        english.put("merchant.pay", "Merchant payment");
        english.put("bills.title", "Bills and top-ups");
        english.put("trading.title", "Trading account");
        english.put("trading.marketOrder", "Market order");
        english.put("trading.limitOrder", "Limit order");
        english.put("loan.title", "Small loans");
        english.put("loan.apply", "Apply for a loan");
        english.put("security.biometricRequired", "Biometric confirmation is required for this transaction.");
        english.put("security.passcodeRequired", "Enter the 6-digit release code.");
        english.put("offline.queued", "Transaction saved offline and will sync when internet returns.");
        english.put("offline.synced", "Offline transaction synced successfully.");
        english.put("support.contactBranch", "Contact your branch for help.");

        Map<String, String> arabic = new LinkedHashMap<>();
        arabic.put("app.name", "المختار إيليت");
        arabic.put("common.language", "اللغة");
        arabic.put("common.direction", "rtl");
        arabic.put("common.continue", "متابعة");
        arabic.put("common.cancel", "إلغاء");
        arabic.put("common.confirm", "تأكيد");
        arabic.put("common.save", "حفظ");
        arabic.put("common.close", "إغلاق");
        arabic.put("common.search", "بحث");
        arabic.put("common.amount", "المبلغ");
        arabic.put("common.currency", "العملة");
        arabic.put("common.status", "الحالة");
        arabic.put("common.branch", "الفرع");
        arabic.put("common.fees", "الأجور");
        arabic.put("common.total", "المجموع");
        arabic.put("auth.login.title", "تسجيل الدخول");
        arabic.put("auth.username", "اسم المستخدم");
        arabic.put("auth.password", "كلمة السر");
        arabic.put("auth.login.submit", "دخول");
        arabic.put("auth.error.invalid", "اسم المستخدم أو كلمة السر غير صحيحة.");
        arabic.put("home.balance", "رصيد المحفظة");
        arabic.put("home.quickTransfer", "إرسال حوالة");
        arabic.put("home.scanQr", "مسح QR");
        arabic.put("home.payBill", "دفع فاتورة");
        arabic.put("home.trade", "التداول");
        arabic.put("transfer.title", "إرسال حوالة");
        arabic.put("transfer.senderBranch", "فرع الإرسال");
        arabic.put("transfer.receiverBranch", "فرع الاستلام");
        arabic.put("transfer.receiverName", "اسم المستلم");
        arabic.put("transfer.receiverPhone", "رقم المستلم");
        arabic.put("transfer.releaseCode", "رمز التسليم");
        arabic.put("transfer.cleanPayout", "المستلم يقبض المبلغ كامل. الأجور محسوبة على فرع الإرسال.");
        arabic.put("transfer.pending", "الحوالة معلقة لحتى يتم التأكد من رمز التسليم.");
        arabic.put("transfer.completed", "تمت الحوالة بنجاح.");
        arabic.put("transfer.failed", "ما تمت الحوالة. حاول مرة تانية أو راجع الفرع.");
        arabic.put("qr.title", "QR الاستلام الذكي");
        arabic.put("qr.instructions", "فرجي هذا الرمز للكاشير بأي فرع من فروع المختار.");
        arabic.put("qr.expired", "انتهت صلاحية رمز QR. اطلب رمز جديد.");
        arabic.put("qr.used", "رمز QR مستخدم من قبل.");
        arabic.put("wallet.title", "محفظتي");
        arabic.put("wallet.topup", "تعبئة المحفظة");
        arabic.put("wallet.exchange", "تصريف عملة");
        arabic.put("wallet.frozen", "المحفظة موقوفة مؤقتا. تواصل مع الدعم.");
        arabic.put("kyc.title", "تأكيد الهوية");
        arabic.put("kyc.uploadId", "ارفع صورة الهوية");
        arabic.put("kyc.selfie", "ارفع صورة سيلفي");
        arabic.put("kyc.pending", "طلب التحقق قيد المراجعة.");
        arabic.put("merchant.pay", "دفع لتاجر");
        arabic.put("bills.title", "الفواتير والتعبئة");
        arabic.put("trading.title", "حساب التداول");
        arabic.put("trading.marketOrder", "أمر سوق");
        arabic.put("trading.limitOrder", "أمر محدد");
        arabic.put("loan.title", "قروض صغيرة");
        arabic.put("loan.apply", "طلب قرض");
        arabic.put("security.biometricRequired", "مطلوب تأكيد بالبصمة أو الوجه لهذه العملية.");
        arabic.put("security.passcodeRequired", "أدخل رمز التسليم المؤلف من 6 أرقام.");
        arabic.put("offline.queued", "تم حفظ العملية بدون إنترنت، وبتتزامن أول ما يرجع الاتصال.");
        arabic.put("offline.synced", "تمت مزامنة العملية بنجاح.");
        arabic.put("support.contactBranch", "راجع فرعك للمساعدة.");

        english.forEach((key, value) -> saveTranslation("en", key, value));
        arabic.forEach((key, value) -> {
            saveTranslation("ar", key, value);
            saveTranslation("ar-SY", key, value);
        });
    }

    private void saveTranslation(String locale, String key, String value) {
        if (translationRepository.findByLocaleAndMessageKey(locale, key).isEmpty()) {
            translationRepository.save(Translation.builder()
                    .locale(locale)
                    .messageKey(key)
                    .messageValue(value)
                    .build());
        }
    }

    private void createBranchCashInventoriesIfNeeded() {
        branchRepository.findAll().forEach(branch -> {
            seedBranchCash(branch, "USD", "250000.0000", "25000.0000", "750000.0000");
            seedBranchCash(branch, "EUR", "100000.0000", "10000.0000", "300000.0000");
            seedBranchCash(branch, "TL", "2000000.0000", "250000.0000", "5000000.0000");
            seedBranchCash(branch, "SYP", "500000000.0000", "50000000.0000", "1500000000.0000");
        });
    }

    private void seedBranchCash(Branch branch, String currency, String available, String lowThreshold, String highThreshold) {
        if (branchCashInventoryRepository.findByBranchIdAndCurrency(branch.getId(), currency).isPresent()) {
            return;
        }
        branchCashInventoryRepository.save(BranchCashInventory.builder()
                .branch(branch)
                .currency(currency)
                .availableBalance(new BigDecimal(available))
                .reservedBalance(BigDecimal.ZERO)
                .lowCashThreshold(new BigDecimal(lowThreshold))
                .highCashThreshold(new BigDecimal(highThreshold))
                .build());
    }
}
