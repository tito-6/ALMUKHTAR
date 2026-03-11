package com.mycompany.transfersystem.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.dto.QrGenerationResponse;
import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.exception.NotificationException;
import com.mycompany.transfersystem.service.AuditService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "whatsapp.api", name = "access-token")
public class WhatsAppNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationProvider.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Value("${whatsapp.api.base-url:https://graph.facebook.com/v20.0}")
    private String baseUrl;

    @Value("${whatsapp.api.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.api.access-token}")
    private String accessToken;

    @Value("${whatsapp.template.transaction-receipt:almukhtar_receipt_v1}")
    private String receiptTemplate;

    @Value("${whatsapp.template.qr-delivery:almukhtar_qr_delivery_v1}")
    private String qrDeliveryTemplate;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuditService auditService;

    public WhatsAppNotificationProvider(AuditService auditService) {
        this.auditService = auditService;
    }

    public void sendTransactionReceipt(Transaction tx, String receiverPhone) {
        if (receiverPhone == null || receiverPhone.isBlank()) return;
        try {
            List<Map<String, String>> params = List.of(
                    Map.of("type", "text", "text", tx.getReceiver().getUsername()),
                    Map.of("type", "text", "text", tx.getAmount().toPlainString()),
                    Map.of("type", "text", "text", tx.getCurrencyCode() != null ? tx.getCurrencyCode() : "USD"),
                    Map.of("type", "text", "text", tx.getId().toString()),
                    Map.of("type", "text", "text", tx.getCreatedAt().toString())
            );
            Map<String, Object> payload = buildTemplatePayload(receiverPhone, receiptTemplate, params);
            sendMessage(payload, "RECEIPT", tx.getId().toString());
        } catch (Exception e) {
            log.error("WhatsApp receipt send failed: {}", e.getMessage());
            auditService.logSystemError("WHATSAPP_SEND_FAILED", "RECEIPT for tx " + tx.getId(), e.getMessage());
        }
    }

    public void sendQrCode(String receiverPhone, QrGenerationResponse qrData, Transaction tx) {
        if (receiverPhone == null || receiverPhone.isBlank()) return;
        try {
            List<Map<String, String>> params = List.of(
                    Map.of("type", "text", "text", tx.getReceiver().getUsername()),
                    Map.of("type", "text", "text", qrData.getExpiresAt().toString())
            );
            Map<String, Object> payload = buildTemplatePayload(receiverPhone, qrDeliveryTemplate, params);
            sendMessage(payload, "QR_DELIVERY", tx.getId().toString());
        } catch (Exception e) {
            log.error("WhatsApp QR delivery failed: {}", e.getMessage());
            auditService.logSystemError("WHATSAPP_SEND_FAILED", "QR_DELIVERY for tx " + tx.getId(), e.getMessage());
        }
    }

    private Map<String, Object> buildTemplatePayload(String toPhone, String templateName, List<Map<String, String>> params) {
        return Map.of(
                "messaging_product", "whatsapp",
                "to", toPhone.replaceAll("[^0-9]", ""),
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", "ar"),
                        "components", List.of(Map.of("type", "body", "parameters", params)))
        );
    }

    private void sendMessage(Map<String, Object> payload, String type, String referenceId) {
        try {
            String url = baseUrl + "/" + phoneNumberId + "/messages";
            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    log.error("WhatsApp API error [{}]: {}", response.code(), errBody);
                    throw new NotificationException("WhatsApp delivery failed for " + type + " #" + referenceId);
                }
                log.info("WhatsApp {} sent for reference {}", type, referenceId);
            }
        } catch (IOException e) {
            log.error("WhatsApp IO error for type {}: {}", type, e.getMessage());
            auditService.logSystemError("WHATSAPP_SEND_FAILED", type + " for ref " + referenceId, e.getMessage());
        }
    }
}
