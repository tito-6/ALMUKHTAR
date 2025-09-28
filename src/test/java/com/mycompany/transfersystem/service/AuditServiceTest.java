package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.CommissionScope;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class AuditServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CommissionRateRepository commissionRateRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    private AuditService auditService;
    private Branch mainAdminBranch;
    private Branch branchA;
    private Branch branchB;
    private User superAdmin;
    private User branchManagerA;
    private User auditor;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(
            transactionRepository,
            fundRepository,
            branchRepository,
            commissionRateRepository,
            auditLogRepository
        );

        // Create test data
        setupTestData();
    }

    private void setupTestData() {
        // Create branches
        mainAdminBranch = new Branch();
        mainAdminBranch.setName("MAIN_ADMIN_BRANCH");
        mainAdminBranch = entityManager.persistAndFlush(mainAdminBranch);

        branchA = new Branch();
        branchA.setName("BRANCH_A");
        branchA = entityManager.persistAndFlush(branchA);

        branchB = new Branch();
        branchB.setName("BRANCH_B");
        branchB = entityManager.persistAndFlush(branchB);

        // Create users
        superAdmin = new User();
        superAdmin.setUsername("admin");
        superAdmin.setPassword("admin123");
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin = entityManager.persistAndFlush(superAdmin);

        branchManagerA = new User();
        branchManagerA.setUsername("manager");
        branchManagerA.setPassword("manager123");
        branchManagerA.setRole(UserRole.BRANCH_MANAGER);
        branchManagerA = entityManager.persistAndFlush(branchManagerA);

        auditor = new User();
        auditor.setUsername("auditor");
        auditor.setPassword("auditor123");
        auditor.setRole(UserRole.AUDITOR);
        auditor = entityManager.persistAndFlush(auditor);

        // Create funds
        Fund platformFund = new Fund();
        platformFund.setName("Platform Fund");
        platformFund.setBalance(new BigDecimal("1000000.00"));
        platformFund.setStatus(FundStatus.ACTIVE);
        entityManager.persistAndFlush(platformFund);

        Fund branchAFund = new Fund();
        branchAFund.setName("BRANCH_A Fund");
        branchAFund.setBalance(new BigDecimal("1000000.00"));
        branchAFund.setStatus(FundStatus.ACTIVE);
        entityManager.persistAndFlush(branchAFund);

        Fund branchBFund = new Fund();
        branchBFund.setName("BRANCH_B Fund");
        branchBFund.setBalance(new BigDecimal("1000000.00"));
        branchBFund.setStatus(FundStatus.ACTIVE);
        entityManager.persistAndFlush(branchBFund);

        // Create commission rates
        CommissionRate platformBaseFee = new CommissionRate();
        platformBaseFee.setBranch(mainAdminBranch);
        platformBaseFee.setCommissionScope(CommissionScope.PLATFORM_BASE_FEE);
        platformBaseFee.setRateValue(new BigDecimal("1.50"));
        entityManager.persistAndFlush(platformBaseFee);

        CommissionRate branchASendingFee = new CommissionRate();
        branchASendingFee.setBranch(branchA);
        branchASendingFee.setCommissionScope(CommissionScope.SENDING_BRANCH_FEE);
        branchASendingFee.setRateValue(new BigDecimal("1.50"));
        entityManager.persistAndFlush(branchASendingFee);

        // Create sample transactions with proper relationships
        Fund generalFund = new Fund();
        generalFund.setName("General Fund");
        generalFund.setBalance(new BigDecimal("100000.00"));
        generalFund.setStatus(FundStatus.ACTIVE);
        generalFund = entityManager.persistAndFlush(generalFund);

        Transaction transaction1 = new Transaction();
        transaction1.setAmount(new BigDecimal("1000.00"));
        transaction1.setStatus(com.mycompany.transfersystem.entity.enums.TransactionStatus.COMPLETED);
        transaction1.setSender(superAdmin);
        transaction1.setReceiver(branchManagerA);
        transaction1.setFund(generalFund);
        entityManager.persistAndFlush(transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setAmount(new BigDecimal("2000.00"));
        transaction2.setStatus(com.mycompany.transfersystem.entity.enums.TransactionStatus.COMPLETED);
        transaction2.setSender(branchManagerA);
        transaction2.setReceiver(auditor);
        transaction2.setFund(generalFund);
        entityManager.persistAndFlush(transaction2);

        // Create audit logs
        AuditLog auditLog1 = new AuditLog();
        auditLog1.setAction("FEE_MODIFICATION");
        auditLog1.setUser(branchManagerA);
        auditLog1.setEntity("CommissionRate");
        auditLog1.setEntityId(1L);
        auditLog1.setCreatedAt(LocalDateTime.now().minusHours(2));
        entityManager.persistAndFlush(auditLog1);

        AuditLog auditLog2 = new AuditLog();
        auditLog2.setAction("FEE_UPDATE");
        auditLog2.setUser(superAdmin);
        auditLog2.setEntity("CommissionRate");
        auditLog2.setEntityId(2L);
        auditLog2.setCreatedAt(LocalDateTime.now().minusHours(1));
        entityManager.persistAndFlush(auditLog2);
    }

    @Test
    void testGetBranchFundStatus() {
        // Test fund status for Branch A
        Map<String, Object> fundStatus = auditService.getBranchFundStatus(branchA.getId());
        
        assertThat(fundStatus).isNotNull();
        assertThat(fundStatus.get("branchId")).isEqualTo(branchA.getId());
        assertThat(fundStatus.get("branchName")).isEqualTo("BRANCH_A");
        assertThat(fundStatus.get("fundName")).isEqualTo("BRANCH_A Fund");
        assertThat(fundStatus.get("currentBalance")).isEqualTo(new BigDecimal("1000000.00"));
        assertThat(fundStatus.get("fundStatus")).isEqualTo(FundStatus.ACTIVE);
        assertThat(fundStatus.get("netPosition")).isEqualTo("CREDIT");
    }

    @Test
    void testSearchTransactions() {
        // Test transaction search
        com.mycompany.transfersystem.dto.TransactionSearchRequest filters = 
            new com.mycompany.transfersystem.dto.TransactionSearchRequest();
        filters.setStatus(com.mycompany.transfersystem.entity.enums.TransactionStatus.COMPLETED);
        filters.setPage(0);
        filters.setSize(10);
        
        org.springframework.data.domain.Page<Transaction> transactions = 
            auditService.searchTransactions(filters);
        
        assertThat(transactions).isNotNull();
        assertThat(transactions.getContent()).hasSize(2);
        assertThat(transactions.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testGetFeeModificationHistory() {
        // Test fee modification history
        List<Map<String, Object>> feeHistory = auditService.getFeeModificationHistory();
        
        assertThat(feeHistory).isNotNull();
        assertThat(feeHistory).hasSize(2);
        
        // Check first entry
        Map<String, Object> firstEntry = feeHistory.get(0);
        assertThat(firstEntry.get("action")).isEqualTo("FEE_MODIFICATION");
        assertThat(firstEntry.get("entityType")).isEqualTo("CommissionRate");
        assertThat(firstEntry.get("user")).isEqualTo("manager");
    }

    @Test
    void testGetPlatformSummary() {
        // Test platform summary
        Map<String, Object> summary = auditService.getPlatformSummary();
        
        assertThat(summary).isNotNull();
        assertThat(summary.get("fundName")).isEqualTo("Platform Fund");
        assertThat(summary.get("currentBalance")).isEqualTo(new BigDecimal("1000000.00"));
        assertThat(summary.get("fundStatus")).isEqualTo(FundStatus.ACTIVE);
        assertThat(summary.get("totalFeesCollected")).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    void testGetBranchTransactionReport() {
        // Test branch transaction report
        Map<String, Object> report = auditService.getBranchTransactionReport(branchA.getId(), 30);
        
        assertThat(report).isNotNull();
        assertThat(report.get("branchId")).isEqualTo(branchA.getId());
        assertThat(report.get("branchName")).isEqualTo("BRANCH_A");
        assertThat(report.get("reportPeriod")).isEqualTo("30 days");
        assertThat(report.get("totalTransactions")).isEqualTo(2);
    }

    @Test
    void testGetBranchCommissionRates() {
        // Test commission rates for branch
        List<CommissionRate> commissionRates = auditService.getBranchCommissionRates(branchA.getId());
        
        assertThat(commissionRates).isNotNull();
        assertThat(commissionRates).hasSize(1);
        assertThat(commissionRates.get(0).getCommissionScope()).isEqualTo(CommissionScope.SENDING_BRANCH_FEE);
        assertThat(commissionRates.get(0).getRateValue()).isEqualTo(new BigDecimal("1.50"));
    }

    @Test
    void testIsBranchManagerAuthorized() {
        // Test branch manager authorization
        boolean authorized = auditService.isBranchManagerAuthorized(branchA.getId(), "manager");
        assertThat(authorized).isTrue();
        
        boolean notAuthorized = auditService.isBranchManagerAuthorized(mainAdminBranch.getId(), "manager");
        assertThat(notAuthorized).isFalse();
    }

    @Test
    void testLogAuditEvent() {
        // Test audit logging
        auditService.log("TEST_ACTION", superAdmin, "TestEntity", 123L);
        
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(3); // 2 from setup + 1 new
        
        AuditLog newLog = auditLogs.stream()
            .filter(log -> "TEST_ACTION".equals(log.getAction()))
            .findFirst()
            .orElse(null);
        
        assertThat(newLog).isNotNull();
        assertThat(newLog.getAction()).isEqualTo("TEST_ACTION");
        assertThat(newLog.getUser().getUsername()).isEqualTo("admin");
        assertThat(newLog.getEntity()).isEqualTo("TestEntity");
        assertThat(newLog.getEntityId()).isEqualTo(123L);
    }
}
