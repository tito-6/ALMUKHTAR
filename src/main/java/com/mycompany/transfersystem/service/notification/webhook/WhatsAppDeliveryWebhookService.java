package com.mycompany.transfersystem.service.notification.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.entity.NotificationDeliveryLog;
import com.mycompany.transfersystem.entity.enums.NotificationDeliveryStatus;
import com.mycompany.transfersystem.repository.NotificationDeliveryLogRepository;
import com.mycompany.transfersystem.service.notification.NotificationDeliveryLogWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class WhatsAppDeliveryWebhookService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppDeliveryWebhookService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final NotificationDeliveryLogWriter logWriter;

    @Value("${almukhtar.whatsapp.webhook-app-secret:${whatsapp.webhook.app-secret:}}")
    private String appSecret;

    public WhatsAppDeliveryWebhookService(NotificationDeliveryLogRepository deliveryLogRepository,
                                          NotificationDeliveryLogWriter logWriter) {
        this.deliveryLogRepository = deliveryLogRepository;
        this.logWriter = logWriter;
    }

    public boolean verifySignature(byte[] rawBody, String signatureHeader) {
        if (!StringUtils.hasText(appSecret)) {
            return true;
        }
        if (!StringUtils.hasText(signatureHeader) || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody);
            String expected = "sha256=" + HexFormat.of().formatHex(hash);
            return constantTimeEquals(expected, signatureHeader);
        } catch (Exception e) {
            log.warn("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    public void handlePayload(byte[] rawBody) throws Exception {
        JsonNode root = objectMapper.readTree(rawBody);
        for (JsonNode entry : root.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                JsonNode statuses = change.path("value").path("statuses");
                if (!statuses.isArray()) {
                    continue;
                }
                for (JsonNode st : statuses) {
                    String id = st.path("id").asText(null);
                    String status = st.path("status").asText(null);
                    if (id == null || status == null) {
                        continue;
                    }
                    applyStatus(id, status);
                }
            }
        }
    }

    private void applyStatus(String providerMessageId, String status) {
        NotificationDeliveryLog logEntry = deliveryLogRepository.findByProviderMessageId(providerMessageId).orElse(null);
        if (logEntry == null) {
            return;
        }
        Instant now = Instant.now();
        switch (status.toLowerCase()) {
            case "sent" -> {
                logEntry.setStatus(NotificationDeliveryStatus.SENT);
                if (logEntry.getSentAt() == null) {
                    logEntry.setSentAt(now);
                }
            }
            case "delivered" -> {
                logEntry.setStatus(NotificationDeliveryStatus.DELIVERED);
                if (logEntry.getDeliveredAt() == null) {
                    logEntry.setDeliveredAt(now);
                }
            }
            case "read" -> {
                logEntry.setStatus(NotificationDeliveryStatus.READ);
                if (logEntry.getReadAt() == null) {
                    logEntry.setReadAt(now);
                }
            }
            case "failed" -> {
                logEntry.setStatus(NotificationDeliveryStatus.FAILED);
                logEntry.setFailureReason("provider_status_failed");
            }
            default -> log.debug("Ignoring unknown WhatsApp status {}", status);
        }
        logWriter.saveUpdate(logEntry);
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
