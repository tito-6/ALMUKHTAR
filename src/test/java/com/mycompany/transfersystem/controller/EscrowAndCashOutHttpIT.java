package com.mycompany.transfersystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.CashOutRequest;
import com.mycompany.transfersystem.entity.EscrowContract;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.WalletBalance;
import com.mycompany.transfersystem.entity.enums.KycTier;
import com.mycompany.transfersystem.entity.enums.WalletStatus;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.CashOutRequestRepository;
import com.mycompany.transfersystem.repository.EscrowContractRepository;
import com.mycompany.transfersystem.repository.LedgerEntryRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.repository.WalletBalanceRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class EscrowAndCashOutHttpIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletBalanceRepository walletBalanceRepository;

    @Autowired
    private EscrowContractRepository escrowContractRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private CashOutRequestRepository cashOutRequestRepository;

    @Autowired
    private BranchRepository branchRepository;

    private MockMvc mockMvc;
    private User initiator;
    private User beneficiary;
    private User outsider;
    private Branch branch;
    private Wallet initiatorWallet;
    private Wallet beneficiaryWallet;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        branch = branchRepository.findFirstByName("BRANCH_A").orElseThrow();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        initiator = createUser("escrow_init_" + suffix);
        beneficiary = createUser("escrow_benef_" + suffix);
        outsider = createUser("escrow_out_" + suffix);
        initiatorWallet = createWallet(initiator, "W-I-" + suffix, new BigDecimal("1000.0000"));
        beneficiaryWallet = createWallet(beneficiary, "W-B-" + suffix, BigDecimal.ZERO);
        createWallet(outsider, "W-O-" + suffix, new BigDecimal("1000.0000"));
    }

    @Test
    void escrowFundAndRelease_areIdempotentAndPostLedgerEntries() throws Exception {
        EscrowContract contract = escrowContractRepository.save(EscrowContract.builder()
                .initiatorUser(initiator)
                .beneficiaryUser(beneficiary)
                .amount(new BigDecimal("125.0000"))
                .currency("USD")
                .conditionType("MANUAL_APPROVAL")
                .status("ACTIVE")
                .build());
        String fundKey = uniqueKey();

        mockMvc.perform(post("/api/escrow/{id}/fund", contract.getId())
                        .with(user(initiator.getUsername()).roles("INDIVIDUAL_USER"))
                        .header("Idempotency-Key", fundKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FUNDED"));

        mockMvc.perform(post("/api/escrow/{id}/fund", contract.getId())
                        .with(user(initiator.getUsername()).roles("INDIVIDUAL_USER"))
                        .header("Idempotency-Key", fundKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FUNDED"));

        assertThat(walletBalance(initiatorWallet)).isEqualByComparingTo("875.0000");
        assertThat(escrowContractRepository.findById(contract.getId()).orElseThrow().getFundedAt()).isNotNull();
        assertThat(ledgerEntryRepository.findAll().stream()
                .filter(e -> "ESCROW_CONTRACT".equals(e.getReferenceType()) && contract.getId().equals(e.getReferenceId()))
                .count()).isEqualTo(2);

        String releaseKey = uniqueKey();
        mockMvc.perform(post("/api/escrow/{id}/release", contract.getId())
                        .with(user(initiator.getUsername()).roles("INDIVIDUAL_USER"))
                        .header("Idempotency-Key", releaseKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));

        mockMvc.perform(post("/api/escrow/{id}/release", contract.getId())
                        .with(user(initiator.getUsername()).roles("INDIVIDUAL_USER"))
                        .header("Idempotency-Key", releaseKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));

        assertThat(walletBalance(beneficiaryWallet)).isEqualByComparingTo("125.0000");
        assertThat(escrowContractRepository.findById(contract.getId()).orElseThrow().getReleasedAt()).isNotNull();
        assertThat(ledgerEntryRepository.findAll().stream()
                .filter(e -> "ESCROW_RELEASE".equals(e.getReferenceType()) && contract.getId().equals(e.getReferenceId()))
                .count()).isEqualTo(2);
    }

    @Test
    void escrowFund_rejectsNonInitiatorWithoutDebit() throws Exception {
        EscrowContract contract = escrowContractRepository.save(EscrowContract.builder()
                .initiatorUser(initiator)
                .beneficiaryUser(beneficiary)
                .amount(new BigDecimal("90.0000"))
                .currency("USD")
                .conditionType("MANUAL_APPROVAL")
                .status("ACTIVE")
                .build());

        mockMvc.perform(post("/api/escrow/{id}/fund", contract.getId())
                        .with(user(outsider.getUsername()).roles("INDIVIDUAL_USER"))
                        .header("Idempotency-Key", uniqueKey()))
                .andExpect(status().isForbidden());

        assertThat(walletBalance(initiatorWallet)).isEqualByComparingTo("1000.0000");
        assertThat(escrowContractRepository.findById(contract.getId()).orElseThrow().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void escrowDispute_allowsEitherPartyAndRequiresIdempotencyKey() throws Exception {
        EscrowContract contract = escrowContractRepository.save(EscrowContract.builder()
                .initiatorUser(initiator)
                .beneficiaryUser(beneficiary)
                .amount(new BigDecimal("50.0000"))
                .currency("USD")
                .conditionType("MANUAL_APPROVAL")
                .status("ACTIVE")
                .build());
        String body = objectMapper.writeValueAsString(Map.of("reasonCategory", "DELIVERY_DELAY"));

        mockMvc.perform(post("/api/escrow/{id}/dispute", contract.getId())
                        .with(user(beneficiary.getUsername()).roles("INDIVIDUAL_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REQUIRED"));

        mockMvc.perform(post("/api/escrow/{id}/dispute", contract.getId())
                        .with(user(beneficiary.getUsername()).roles("INDIVIDUAL_USER"))
                        .header("Idempotency-Key", uniqueKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPUTED"));
    }

    @Test
    void cashOutRequestAndComplete_areIdempotentAcrossHttpBoundary() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "branchId", branch.getId(),
                "amount", new BigDecimal("40.0000"),
                "currency", "USD"));
        String requestKey = uniqueKey();

        MvcResult firstRequest = mockMvc.perform(post("/api/wallet/cashout/request")
                        .with(user(initiator.getUsername()).roles("INDIVIDUAL_USER"))
                        .header("Idempotency-Key", requestKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        MvcResult replayedRequest = mockMvc.perform(post("/api/wallet/cashout/request")
                        .with(user(initiator.getUsername()).roles("INDIVIDUAL_USER"))
                        .header("Idempotency-Key", requestKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Long cashOutId = objectMapper.readTree(firstRequest.getResponse().getContentAsString()).get("id").asLong();
        Long replayedId = objectMapper.readTree(replayedRequest.getResponse().getContentAsString()).get("id").asLong();
        assertThat(replayedId).isEqualTo(cashOutId);
        assertThat(cashOutRequestRepository.findAll().stream()
                .filter(r -> initiatorWallet.getId().equals(r.getWallet().getId()) && r.getAmount().compareTo(new BigDecimal("40.0000")) == 0)
                .count()).isEqualTo(1);

        mockMvc.perform(put("/api/wallet/cashout/{id}/complete", cashOutId)
                        .with(user(beneficiary.getUsername()).roles("INDIVIDUAL_USER"))
                        .header("Idempotency-Key", uniqueKey()))
                .andExpect(status().isForbidden());

        String completeKey = uniqueKey();
        mockMvc.perform(put("/api/wallet/cashout/{id}/complete", cashOutId)
                        .with(user("cashier").roles("CASHIER"))
                        .header("Idempotency-Key", completeKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(put("/api/wallet/cashout/{id}/complete", cashOutId)
                        .with(user("cashier").roles("CASHIER"))
                        .header("Idempotency-Key", completeKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(walletBalance(initiatorWallet)).isEqualByComparingTo("960.0000");
        CashOutRequest completed = cashOutRequestRepository.findById(cashOutId).orElseThrow();
        assertThat(completed.getCompletedAt()).isNotNull();
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("pass123"));
        user.setRole(com.mycompany.transfersystem.entity.enums.UserRole.INDIVIDUAL_USER);
        user.setPhone("+9639" + username.hashCode());
        return userRepository.save(user);
    }

    private Wallet createWallet(User user, String walletNumber, BigDecimal balance) {
        Wallet wallet = walletRepository.save(Wallet.builder()
                .user(user)
                .walletNumber(walletNumber)
                .status(WalletStatus.ACTIVE)
                .kycTier(KycTier.BASIC)
                .build());
        walletBalanceRepository.save(WalletBalance.builder()
                .wallet(wallet)
                .currencyCode("USD")
                .availableBalance(balance)
                .lockedBalance(BigDecimal.ZERO)
                .build());
        return wallet;
    }

    private BigDecimal walletBalance(Wallet wallet) {
        return walletBalanceRepository.findByWalletIdAndCurrencyCode(wallet.getId(), "USD")
                .orElseThrow()
                .getAvailableBalance();
    }

    private String uniqueKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
