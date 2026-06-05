package com.mycompany.transfersystem.service.notification.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppDeliveryWebhookServiceTest {

    @Test
    void verifySignature_rejectsInvalidSignatureWhenSecretConfigured() {
        WhatsAppDeliveryWebhookService svc = new WhatsAppDeliveryWebhookService(null, null);
        ReflectionTestUtils.setField(svc, "appSecret", "mysecret");

        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        assertThat(svc.verifySignature(body, "sha256=deadbeef")).isFalse();
        assertThat(svc.verifySignature(body, null)).isFalse();
    }

    @Test
    void verifySignature_acceptsValidHmac() {
        WhatsAppDeliveryWebhookService svc = new WhatsAppDeliveryWebhookService(null, null);
        ReflectionTestUtils.setField(svc, "appSecret", "mysecret");

        byte[] body = "{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        javax.crypto.Mac mac;
        try {
            mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec("mysecret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String hex = java.util.HexFormat.of().formatHex(mac.doFinal(body));
        assertThat(svc.verifySignature(body, "sha256=" + hex)).isTrue();
    }
}
