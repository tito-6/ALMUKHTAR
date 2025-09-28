package com.mycompany.transfersystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.dto.FeeUpdateRequest;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.CommissionRate;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.CommissionScope;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.CommissionRateRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.AuthService;
import com.mycompany.transfersystem.service.FeeManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class FeeConfigurationIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CommissionRateRepository commissionRateRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private User superAdmin;
    private User branchManagerA;
    private User branchManagerB;
    private Branch mainAdminBranch;
    private Branch branchA;
    private Branch branchB;
    private String superAdminToken;
    private String branchManagerAToken;
    private String branchManagerBToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        setupTestData();
        setupTokens();
    }

    private void setupTestData() {
        // Use existing branches created by DataInitializer
        mainAdminBranch = branchRepository.findFirstByName("MAIN_ADMIN_BRANCH")
                .orElseThrow(() -> new RuntimeException("MAIN_ADMIN_BRANCH not found"));

        branchA = branchRepository.findFirstByName("BRANCH_A")
                .orElseThrow(() -> new RuntimeException("BRANCH_A not found"));

        branchB = branchRepository.findFirstByName("BRANCH_B")
                .orElseThrow(() -> new RuntimeException("BRANCH_B not found"));

        // Use existing users created by DataInitializer
        superAdmin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new RuntimeException("admin user not found"));

        branchManagerA = userRepository.findByUsername("manager")
                .orElseThrow(() -> new RuntimeException("manager user not found"));

        // Create a second branch manager for testing
        branchManagerB = new User();
        branchManagerB.setUsername("managerB");
        branchManagerB.setPassword(passwordEncoder.encode("manager123"));
        branchManagerB.setRole(UserRole.BRANCH_MANAGER);
        branchManagerB = userRepository.save(branchManagerB);

        // Commission rates are already created by DataInitializer
    }

    private void createCommissionRate(Branch branch, CommissionScope scope, BigDecimal rate) {
        CommissionRate commissionRate = new CommissionRate();
        commissionRate.setBranch(branch);
        commissionRate.setCommissionScope(scope);
        commissionRate.setRateValue(rate);
        commissionRateRepository.save(commissionRate);
    }

    private void setupTokens() throws Exception {
        // Get tokens for different users
        superAdminToken = getAuthToken("admin", "admin123");
        branchManagerAToken = getAuthToken("manager", "manager123");
        branchManagerBToken = getAuthToken("managerB", "manager123");
    }

    private String getAuthToken(String username, String password) throws Exception {
        String loginRequest = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        
        String response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Extract token from response (assuming it returns JSON with "token" field)
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    public void testSuperAdminCanModifyPlatformFee() throws Exception {
        // Test 1: SUPER_ADMIN Success - Change PLATFORM_EXCHANGE_PROFIT from 1.50 to 2.00
        FeeUpdateRequest request = new FeeUpdateRequest();
        request.setNewRate(new BigDecimal("2.00"));

        mockMvc.perform(put("/api/admin/fees/{branchId}/{scope}", mainAdminBranch.getId(), CommissionScope.PLATFORM_EXCHANGE_PROFIT)
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateValue").value(2.00))
                .andExpect(jsonPath("$.commissionScope").value("PLATFORM_EXCHANGE_PROFIT"));

        // Verify the change was persisted
        CommissionRate updatedRate = commissionRateRepository
                .findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_EXCHANGE_PROFIT)
                .orElseThrow();
        assertThat(updatedRate.getRateValue()).isEqualByComparingTo("2.00");
    }

    @Test
    public void testBranchManagerCanModifyOwnBranchFee() throws Exception {
        // Test 2: BRANCH_MANAGER Success - Change SENDING_BRANCH_FEE for BRANCH_A from 1.50 to 1.80
        FeeUpdateRequest request = new FeeUpdateRequest();
        request.setNewRate(new BigDecimal("1.80"));

        mockMvc.perform(put("/api/admin/fees/{branchId}/{scope}", branchA.getId(), CommissionScope.SENDING_BRANCH_FEE)
                .header("Authorization", "Bearer " + branchManagerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateValue").value(1.80))
                .andExpect(jsonPath("$.commissionScope").value("SENDING_BRANCH_FEE"));

        // Verify the change was persisted
        CommissionRate updatedRate = commissionRateRepository
                .findByBranchAndCommissionScope(branchA, CommissionScope.SENDING_BRANCH_FEE)
                .orElseThrow();
        assertThat(updatedRate.getRateValue()).isEqualByComparingTo("1.80");
    }

    @Test
    public void testBranchManagerCannotModifyOtherBranchFee() throws Exception {
        // Test 3: BRANCH_MANAGER Failure - Wrong Branch
        // BRANCH_MANAGER for BRANCH_A tries to modify RECEIVING_BRANCH_FEE for BRANCH_B
        FeeUpdateRequest request = new FeeUpdateRequest();
        request.setNewRate(new BigDecimal("5.00"));

        mockMvc.perform(put("/api/admin/fees/{branchId}/{scope}", branchB.getId(), CommissionScope.RECEIVING_BRANCH_FEE)
                .header("Authorization", "Bearer " + branchManagerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify the rate was NOT changed
        CommissionRate unchangedRate = commissionRateRepository
                .findByBranchAndCommissionScope(branchB, CommissionScope.RECEIVING_BRANCH_FEE)
                .orElseThrow();
        assertThat(unchangedRate.getRateValue()).isEqualByComparingTo("4.00");
    }

    @Test
    public void testBranchManagerCannotModifyPlatformFee() throws Exception {
        // Test 4: BRANCH_MANAGER Failure - Platform Fee
        // BRANCH_MANAGER for BRANCH_A tries to modify PLATFORM_BASE_FEE for MAIN_ADMIN_BRANCH
        FeeUpdateRequest request = new FeeUpdateRequest();
        request.setNewRate(new BigDecimal("2.00"));

        mockMvc.perform(put("/api/admin/fees/{branchId}/{scope}", mainAdminBranch.getId(), CommissionScope.PLATFORM_BASE_FEE)
                .header("Authorization", "Bearer " + branchManagerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Verify the rate was NOT changed
        CommissionRate unchangedRate = commissionRateRepository
                .findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE)
                .orElseThrow();
        assertThat(unchangedRate.getRateValue()).isEqualByComparingTo("1.50");
    }

    @Test
    public void testUnauthorizedUserCannotAccessEndpoint() throws Exception {
        // Test with no authentication
        FeeUpdateRequest request = new FeeUpdateRequest();
        request.setNewRate(new BigDecimal("2.00"));

        mockMvc.perform(put("/api/admin/fees/{branchId}/{scope}", mainAdminBranch.getId(), CommissionScope.PLATFORM_BASE_FEE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetCommissionRate() throws Exception {
        // Test GET endpoint for retrieving commission rate
        mockMvc.perform(get("/api/admin/fees/{branchId}/{scope}", branchA.getId(), CommissionScope.SENDING_BRANCH_FEE)
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateValue").value(1.50))
                .andExpect(jsonPath("$.commissionScope").value("SENDING_BRANCH_FEE"));
    }

    @Test
    public void testGetAllCommissionRatesForBranch() throws Exception {
        // Test GET endpoint for retrieving all commission rates for a branch
        mockMvc.perform(get("/api/admin/fees/{branchId}", branchA.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)) // SENDING and RECEIVING fees
                .andExpect(jsonPath("$[0].commissionScope").value("SENDING_BRANCH_FEE"))
                .andExpect(jsonPath("$[1].commissionScope").value("RECEIVING_BRANCH_FEE"));
    }
}
