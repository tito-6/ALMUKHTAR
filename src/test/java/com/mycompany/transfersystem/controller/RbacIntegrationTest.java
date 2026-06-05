package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RbacIntegrationTest extends AbstractIntegrationTest {

    @Test
    void missingJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }
}
