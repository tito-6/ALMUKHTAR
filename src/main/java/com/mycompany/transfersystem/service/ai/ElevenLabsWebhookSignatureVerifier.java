package com.mycompany.transfersystem.service.ai;

import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Verifies ElevenLabs-style webhook signatures: header contains {@code t=timestamp,v0=hexhmac}
 * over {@code timestamp + "." + rawBody} using HMAC-SHA256(webhookSecret).
 */
public final class ElevenLabsWebhookSignatureVerifier {

    private static final long MAX_SKEW_SECONDS = 30 * 60;

    private ElevenLabsWebhookSignatureVerifier() {}

    public static boolean isValid(String rawBody, String signatureHeader, String webhookSecret) {
        if (rawBody == null || !StringUtils.hasText(signatureHeader) || !StringUtils.hasText(webhookSecret)) {
            return false;
        }
        String timestamp = null;
        String v0 = null;
        for (String part : signatureHeader.split(",")) {
            String p = part.trim();
            if (p.startsWith("t=")) {
                timestamp = p.substring(2);
            } else if (p.startsWith("v0=")) {
                v0 = p.substring(3);
            }
        }
        if (timestamp == null || v0 == null) {
            return false;
        }
        try {
            long t = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();
            if (Math.abs(now - t) > MAX_SKEW_SECONDS) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        String payload = timestamp + "." + rawBody;
        String expected = hmacHex(webhookSecret, payload);
        return constantTimeEquals(expected, v0);
    }

    private static String hmacHex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
