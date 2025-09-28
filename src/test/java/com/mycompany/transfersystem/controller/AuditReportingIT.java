package com.mycompany.transfersystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.dto.FeeUpdateRequest;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.CommissionRate;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.CommissionScope;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.AuditLogRepository;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.CommissionRateRepository;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(com.mycompany.transfersystem.config.TestSecurityConfig.class)
public class AuditReportingIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private CommissionRateRepository commissionRateRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Branch mainAdminBranch;
    private Branch branchA;
    private Branch branchB;
    private User superAdmin;
    private User branchManagerA;
    private User branchManagerB;
    private User auditor;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // Use existing branches from DataInitializer
        mainAdminBranch = branchRepository.findFirstByName("MAIN_ADMIN_BRANCH")
                .orElseThrow(() -> new RuntimeException("MAIN_ADMIN_BRANCH not found"));
        branchA = branchRepository.findFirstByName("BRANCH_A")
                .orElseThrow(() -> new RuntimeException("BRANCH_A not found"));
        branchB = branchRepository.findFirstByName("BRANCH_B")
                .orElseThrow(() -> new RuntimeException("BRANCH_B not found"));

        // Use existing users from DataInitializer
        superAdmin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new RuntimeException("admin user not found"));
        branchManagerA = userRepository.findByUsername("manager")
                .orElseThrow(() -> new RuntimeException("manager user not found"));
        auditor = userRepository.findByUsername("auditor")
                .orElseThrow(() -> new RuntimeException("auditor user not found"));

        // Create additional test user for Branch B
        branchManagerB = new User();
        branchManagerB.setUsername("managerB");
        branchManagerB.setPassword(passwordEncoder.encode("manager123"));
        branchManagerB.setRole(UserRole.BRANCH_MANAGER);
        branchManagerB = userRepository.save(branchManagerB);

        // Create test funds
        createTestFunds();
    }

    private void createTestFunds() {
        // Create Platform Fund (required for platform summary)
        Fund platformFund = new Fund();
        platformFund.setName("Platform Fund");
        platformFund.setBalance(new BigDecimal("1000000.00"));
        platformFund.setStatus(FundStatus.ACTIVE);
        fundRepository.save(platformFund);

        // Create Branch A Fund
        Fund branchAFund = new Fund();
        branchAFund.setName("BRANCH_A Fund");
        branchAFund.setBalance(new BigDecimal("1000000.00"));
        branchAFund.setStatus(FundStatus.ACTIVE);
        fundRepository.save(branchAFund);

        // Create Branch B Fund
        Fund branchBFund = new Fund();
        branchBFund.setName("BRANCH_B Fund");
        branchBFund.setBalance(new BigDecimal("1000000.00"));
        branchBFund.setStatus(FundStatus.ACTIVE);
        fundRepository.save(branchBFund);
        
        // Create some sample audit logs for fee history
        createSampleAuditLogs();
    }
    
    private void createSampleAuditLogs() {
        // Create sample audit logs for fee modification history
        com.mycompany.transfersystem.entity.AuditLog auditLog1 = new com.mycompany.transfersystem.entity.AuditLog();
        auditLog1.setAction("FEE_MODIFICATION");
        auditLog1.setUser(branchManagerA);
        auditLog1.setEntity("CommissionRate");
        auditLog1.setEntityId(1L);
        auditLogRepository.save(auditLog1);
        
        com.mycompany.transfersystem.entity.AuditLog auditLog2 = new com.mycompany.transfersystem.entity.AuditLog();
        auditLog2.setAction("FEE_MODIFICATION");
        auditLog2.setUser(superAdmin);
        auditLog2.setEntity("CommissionRate");
        auditLog2.setEntityId(2L);
        auditLogRepository.save(auditLog2);
    }

    // Test 1: Branch Manager Fund Access
    @Test
    @WithMockUser(username = "manager", roles = {"BRANCH_MANAGER"})
    void testBranchManagerFundAccess() throws Exception {
        // Test 1a: BRANCH_MANAGER can access their own branch fund
        mockMvc.perform(get("/api/audit/funds/{branchId}", branchA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId").value(branchA.getId()))
                .andExpect(jsonPath("$.branchName").value("BRANCH_A"))
                .andExpect(jsonPath("$.currentBalance").exists());

        // Test 1b: BRANCH_MANAGER cannot access other branch funds
        mockMvc.perform(get("/api/audit/funds/{branchId}", branchB.getId()))
                .andExpect(status().isBadRequest()); // Will return 400 due to RuntimeException in service
    }

    // Test 1c: Super Admin Fund Access
    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testSuperAdminFundAccess() throws Exception {
        // SUPER_ADMIN can access any branch fund
        mockMvc.perform(get("/api/audit/funds/{branchId}", branchA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId").value(branchA.getId()));
    }

    // Test 2: Transaction Filtering
    @Test
    @WithMockUser(username = "auditor", roles = {"AUDITOR"})
    void testTransactionFiltering() throws Exception {
        // AUDITOR can search transactions
        mockMvc.perform(get("/api/audit/transactions/search")
                        .param("senderBranchId", branchA.getId().toString())
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // Test 2b: Super Admin Transaction Filtering
    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testSuperAdminTransactionFiltering() throws Exception {
        // SUPER_ADMIN can search transactions
        mockMvc.perform(get("/api/audit/transactions/search")
                        .param("sourceCurrency", "USD")
                        .param("destinationCurrency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // Test 2c: Branch Manager Transaction Filtering (Should Fail)
    @Test
    @WithMockUser(username = "manager", roles = {"BRANCH_MANAGER"})
    void testBranchManagerTransactionFiltering() throws Exception {
        // BRANCH_MANAGER cannot search transactions
        mockMvc.perform(get("/api/audit/transactions/search"))
                .andExpect(status().isForbidden());
    }

    // Test 3: Fee History Check
    @Test
    @WithMockUser(username = "auditor", roles = {"AUDITOR"})
    void testFeeHistoryCheck() throws Exception {
        // AUDITOR can view fee modification history
        mockMvc.perform(get("/api/audit/fees/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // Test 3b: Super Admin Fee History
    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testSuperAdminFeeHistory() throws Exception {
        // SUPER_ADMIN can view fee modification history
        mockMvc.perform(get("/api/audit/fees/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // Test 3c: Branch Manager Fee History (Should Fail)
    @Test
    @WithMockUser(username = "manager", roles = {"BRANCH_MANAGER"})
    void testBranchManagerFeeHistory() throws Exception {
        // BRANCH_MANAGER cannot view fee modification history
        mockMvc.perform(get("/api/audit/fees/history"))
                .andExpect(status().isForbidden());
    }

    // Test 4: Super Admin Summary
    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testSuperAdminSummary() throws Exception {
        // SUPER_ADMIN can view platform summary
        mockMvc.perform(get("/api/audit/platform-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundName").value("Platform Fund"))
                .andExpect(jsonPath("$.currentBalance").exists())
                .andExpect(jsonPath("$.totalFeesCollected").exists());
    }

    // Test 4b: Auditor Platform Summary (Should Fail)
    @Test
    @WithMockUser(username = "auditor", roles = {"AUDITOR"})
    void testAuditorPlatformSummary() throws Exception {
        // AUDITOR cannot view platform summary
        mockMvc.perform(get("/api/audit/platform-summary"))
                .andExpect(status().isForbidden());
    }

    // Test 4c: Branch Manager Platform Summary (Should Fail)
    @Test
    @WithMockUser(username = "manager", roles = {"BRANCH_MANAGER"})
    void testBranchManagerPlatformSummary() throws Exception {
        // BRANCH_MANAGER cannot view platform summary
        mockMvc.perform(get("/api/audit/platform-summary"))
                .andExpect(status().isForbidden());
    }

    // Additional Tests

    @Test
    @WithMockUser(username = "manager", roles = {"BRANCH_MANAGER"})
    void testBranchTransactionReport() throws Exception {
        // Test branch transaction report access
        mockMvc.perform(get("/api/audit/branches/{branchId}/transactions", branchA.getId())
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId").value(branchA.getId()))
                .andExpect(jsonPath("$.branchName").value("BRANCH_A"))
                .andExpect(jsonPath("$.totalTransactions").exists());
    }

    @Test
    @WithMockUser(username = "manager", roles = {"BRANCH_MANAGER"})
    void testBranchCommissionRates() throws Exception {
        // Test commission rates access
        mockMvc.perform(get("/api/audit/branches/{branchId}/commission-rates", branchA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "auditor", roles = {"AUDITOR"})
    void testAuditLogs() throws Exception {
        // Test audit logs access
        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        // Test unauthorized access without token - expecting 403 (Forbidden) not 401 (Unauthorized)
        // because Spring Security is processing through the filter chain and enforcing @PreAuthorize
        mockMvc.perform(get("/api/audit/funds/{branchId}", branchA.getId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit/transactions/search"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit/fees/history"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit/platform-summary"))
                .andExpect(status().isForbidden());
    }

    // Test Fee Modification (to create history)
    @Test
    @WithMockUser(username = "manager", roles = {"BRANCH_MANAGER"})
    void testFeeModification() throws Exception {
        FeeUpdateRequest updateRequest = new FeeUpdateRequest(new BigDecimal("1.80"));
        
        mockMvc.perform(put("/api/admin/fees/{branchId}/{scope}", branchA.getId(), CommissionScope.SENDING_BRANCH_FEE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }
}