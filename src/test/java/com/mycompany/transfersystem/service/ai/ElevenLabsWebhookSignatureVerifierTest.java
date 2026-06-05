package com.mycompany.transfersystem.service.ai;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class ElevenLabsWebhookSignatureVerifierTest {

    @Test
    void acceptsValidSignature() throws Exception {
        String secret = "whsec_unit";
        String body = "{\"conversation_id\":\"x\"}";
        long t = Instant.now().getEpochSecond();
        String sigHeader = "t=" + t + ",v0=" + hmacHex(secret, t + "." + body);
        assertThat(ElevenLabsWebhookSignatureVerifier.isValid(body, sigHeader, secret)).isTrue();
    }

    @Test
    void rejectsInvalidSignature() {
        String body = "{}";
        assertThat(ElevenLabsWebhookSignatureVerifier.isValid(body, "t=1,v0=abc", "secret")).isFalse();
    }

    private static String hmacHex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
