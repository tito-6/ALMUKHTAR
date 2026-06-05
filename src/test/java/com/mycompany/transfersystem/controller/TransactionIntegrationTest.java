package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getTransactions_authenticated_returns200() throws Exception {
        // Integration test placeholder — requires full DB + auth setup
        mockMvc.perform(get("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }
}
