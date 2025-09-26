package com.mycompany.transfersystem.config;

import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.UserRepository;
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
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
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
}