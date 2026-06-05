package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.AbstractIntegrationTest;
import com.mycompany.transfersystem.dto.ai.AiHelperRequest;
import com.mycompany.transfersystem.entity.enums.AiAgentRole;
import com.mycompany.transfersystem.entity.enums.AiToolName;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.AiToolCallAuditRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiHelperIntegrationTest extends AbstractIntegrationTest {

    private static final String WEBHOOK_SECRET = "whsec_test_integration";

    @DynamicPropertySource
    static void elevenLabsProps(DynamicPropertyRegistry r) {
        r.add("almukhtar.elevenlabs.enabled", () -> "true");
        r.add("almukhtar.elevenlabs.webhook-secret", () -> WEBHOOK_SECRET);
        r.add("almukhtar.elevenlabs.api-key", () -> "dummy");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AiToolCallAuditRepository aiToolCallAuditRepository;

    @Test
    void customerTool_withForeignUserId_isForbidden() throws Exception {
        var u = new com.mycompany.transfersystem.entity.User();
        u.setUsername("ai_customer_" + UUID.randomUUID().toString().substring(0, 8));
        u.setPassword(passwordEncoder.encode("cust123"));
        u.setRole(UserRole.INDIVIDUAL_USER);
        userRepository.save(u);

        String token = loginAs(u.getUsername(), "cust123");
        long before = aiToolCallAuditRepository.count();

        AiHelperRequest req = AiHelperRequest.builder()
                .message("balance")
                .agentRole(AiAgentRole.CUSTOMER_HELPER)
                .structuredToolCall(AiHelperRequest.StructuredToolCall.builder()
                        .tool(AiToolName.GET_MY_WALLET_SUMMARY)
                        .arguments(Map.of("targetUserId", "99999"))
                        .build())
                .build();

        mockMvc.perform(post("/api/ai/helper/message")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        assertThat(aiToolCallAuditRepository.count()).isEqualTo(before);
    }

    @Test
    void cashier_cannot_call_platform_revenue_tool() throws Exception {
        String token = loginAs("cashier", "cashier123");
        AiHelperRequest req = AiHelperRequest.builder()
                .message("revenue")
                .agentRole(AiAgentRole.CASHIER_COPILOT)
                .structuredToolCall(AiHelperRequest.StructuredToolCall.builder()
                        .tool(AiToolName.GET_PLATFORM_REVENUE_SUMMARY)
                        .arguments(Map.of())
                        .build())
                .build();

        mockMvc.perform(post("/api/ai/helper/message")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void elevenLabsWebhook_rejectsBadSignature() throws Exception {
        String body = "{\"conversation_id\":\"conv_test_1\"}";
        mockMvc.perform(post("/api/webhooks/elevenlabs/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ElevenLabs-Signature", "t=1,v0=deadbeef")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elevenLabsWebhook_acceptsValidSignature() throws Exception {
        String body = "{\"conversation_id\":\"orphan_conv\"}";
        long t = java.time.Instant.now().getEpochSecond();
        String payload = t + "." + body;
        String v0 = hmacHex(WEBHOOK_SECRET, payload);
        String sig = "t=" + t + ",v0=" + v0;

        mockMvc.perform(post("/api/webhooks/elevenlabs/conversation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("ElevenLabs-Signature", sig)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void structuredTool_call_is_audited() throws Exception {
        String token = loginAs("admin", "admin123");
        long before = aiToolCallAuditRepository.count();

        AiHelperRequest req = AiHelperRequest.builder()
                .message("rates")
                .agentRole(AiAgentRole.PLATFORM_OWNER_COPILOT)
                .structuredToolCall(AiHelperRequest.StructuredToolCall.builder()
                        .tool(AiToolName.GET_MARKET_QUOTE)
                        .arguments(Map.of("fromCurrency", "USD", "toCurrency", "EUR"))
                        .build())
                .build();

        mockMvc.perform(post("/api/ai/helper/message")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertThat(aiToolCallAuditRepository.count()).isGreaterThan(before);
    }

    private static String hmacHex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
