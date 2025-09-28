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
    }

    private void createSampleUsers() {
        // Super Admin
        User superAdmin = new User();
        superAdmin.setUsername("admin");
        superAdmin.setPassword(passwordEncoder.encode("admin123"));
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        userRepository.save(superAdmin);

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
        System.out.println("- admin/admin123 (SUPER_ADMIN)");
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
        System.out.println("- Branch A Sending Fee: $1.50/1000 USD");
        System.out.println("- Branch A Receiving Fee: $4.00/1000 USD");
        System.out.println("- Branch B Sending Fee: $1.50/1000 USD");
        System.out.println("- Branch B Receiving Fee: $4.00/1000 USD");
    }
}