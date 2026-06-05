package com.mycompany.transfersystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.dto.TransferRequest;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.IdempotencyRecord;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.IdempotencyRecordStatus;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.IdempotencyRecordRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class IdempotencyHttpIT {

    private String uniqueKey() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private MockMvc mockMvc;

    private String managerToken;
    private User manager;
    private User cashier;
    private Fund generalFund;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        manager = userRepository.findByUsername("manager").orElseThrow();
        cashier = userRepository.findByUsername("cashier").orElseThrow();
        generalFund = fundRepository.findAll().stream()
                .filter(f -> "General Fund".equals(f.getName()))
                .findFirst()
                .orElseThrow();
        managerToken = login("manager", "manager123");
    }

    private String login(String user, String pass) throws Exception {
        String body = "{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}";
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void missingIdempotencyKey_returns400() throws Exception {
        TransferRequest tr = new TransferRequest(cashier.getId(), manager.getId(), generalFund.getId(), new BigDecimal("10.00"));
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    void sameKeySameBody_replaysWithoutDoubleFundDebit() throws Exception {
        String idemKey = uniqueKey();
        BigDecimal before = fundRepository.findById(generalFund.getId()).orElseThrow().getBalance();
        TransferRequest tr = new TransferRequest(cashier.getId(), manager.getId(), generalFund.getId(), new BigDecimal("11.00"));
        String json = objectMapper.writeValueAsString(tr);

        MvcResult first = mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode id1 = objectMapper.readTree(first.getResponse().getContentAsString()).get("id");

        MvcResult second = mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode id2 = objectMapper.readTree(second.getResponse().getContentAsString()).get("id");

        assertThat(id1.asLong()).isEqualTo(id2.asLong());
        BigDecimal after = fundRepository.findById(generalFund.getId()).orElseThrow().getBalance();
        assertThat(after).isEqualByComparingTo(before.subtract(new BigDecimal("11.00")));
    }

    @Test
    void sameKeyDifferentBody_returns409() throws Exception {
        String idemKey = uniqueKey();
        TransferRequest tr1 = new TransferRequest(cashier.getId(), manager.getId(), generalFund.getId(), new BigDecimal("12.00"));
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tr1)))
                .andExpect(status().isCreated());

        IdempotencyRecord row = idempotencyRecordRepository.findAll().stream()
                .filter(r -> idemKey.equals(r.getIdempotencyKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected idempotency row"));
        assertThat(row.getStatus()).as("first request should persist COMPLETED idempotency").isEqualTo(IdempotencyRecordStatus.COMPLETED);

        TransferRequest tr2 = new TransferRequest(cashier.getId(), manager.getId(), generalFund.getId(), new BigDecimal("13.00"));
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tr2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_BODY_MISMATCH"));
    }

    @Test
    void idempotencyScopedByUser() throws Exception {
        String cashierToken = login("cashier", "cashier123");
        TransferRequest tr = new TransferRequest(manager.getId(), cashier.getId(), generalFund.getId(), new BigDecimal("14.00"));
        String json = objectMapper.writeValueAsString(tr);
        String sharedKey = uniqueKey();

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + managerToken)
                        .header("Idempotency-Key", sharedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + cashierToken)
                        .header("Idempotency-Key", sharedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }
}
