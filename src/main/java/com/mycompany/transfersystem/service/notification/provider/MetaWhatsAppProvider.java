package com.mycompany.transfersystem.service.notification.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.service.notification.util.NotificationPhoneMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Meta WhatsApp Cloud API client. Does not log tokens, full phone numbers, or message bodies containing secrets.
 */
public class MetaWhatsAppProvider implements WhatsAppProvider {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsAppProvider.class);

    private final WebClient webClient;
    private final String phoneNumberId;
    private final String accessToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetaWhatsAppProvider(WebClient webClient,
                                String phoneNumberId,
                                String accessToken) {
        this.webClient = webClient;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
    }

    @Override
    public WhatsAppSendResult sendTemplate(String toE164Phone,
                                           String metaTemplateName,
                                           String languageCode,
                                           List<Map<String, String>> bodyParametersOrdered) {
        String to = NotificationPhoneMask.toE164Digits(toE164Phone);
        List<Map<String, Object>> parameters = new ArrayList<>();
        for (Map<String, String> p : bodyParametersOrdered) {
            Map<String, Object> component = new HashMap<>();
            component.put("type", "text");
            component.put("text", p.get("text"));
            parameters.add(component);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "template");
        payload.put("template", Map.of(
                "name", metaTemplateName,
                "language", Map.of("code", languageCode == null ? "en" : languageCode),
                "components", List.of(Map.of("type", "body", "parameters", parameters))
        ));
        return postMessages(payload, to);
    }

    @Override
    public WhatsAppSendResult sendText(String toE164Phone, String textBody) {
        String to = NotificationPhoneMask.toE164Digits(toE164Phone);
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("preview_url", false, "body", textBody)
        );
        return postMessages(payload, to);
    }

    @Override
    public WhatsAppSendResult sendMedia(String toE164Phone, String mediaType, String mediaUrl, String caption) {
        return WhatsAppSendResult.fail("media_send_not_configured", null, false);
    }

    @Override
    public Optional<String> getDeliveryStatus(String providerMessageId) {
        if (providerMessageId == null || providerMessageId.isBlank()) {
            return Optional.empty();
        }
        try {
            String path = "/" + providerMessageId + "?fields=status";
            JsonNode node = webClient.get()
                    .uri(path)
                    .header("Authorization", bearer())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (node != null && node.hasNonNull("status")) {
                return Optional.of(node.get("status").asText());
            }
        } catch (Exception e) {
            log.warn("Meta WhatsApp status lookup failed for message id hash context: {}", safeId(providerMessageId), e);
        }
        return Optional.empty();
    }

    private WhatsAppSendResult postMessages(Map<String, Object> payload, String toDigits) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            String response = webClient.post()
                    .uri("/" + phoneNumberId + "/messages")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode root = objectMapper.readTree(response);
            JsonNode messages = root.path("messages");
            if (messages.isArray() && !messages.isEmpty()) {
                String id = messages.get(0).path("id").asText(null);
                log.info("Meta WhatsApp message accepted to={}", NotificationPhoneMask.mask(toDigits));
                return WhatsAppSendResult.ok(id);
            }
            log.error("Meta WhatsApp unexpected response shape (no messages array)");
            return WhatsAppSendResult.fail("unexpected_response", 200, false);
        } catch (WebClientResponseException w) {
            boolean t = WhatsAppSendResult.isTransientHttp(w.getStatusCode());
            log.warn("Meta WhatsApp HTTP error status={} transient={}", w.getStatusCode().value(), t);
            return WhatsAppSendResult.fail(w.getStatusText(), w.getStatusCode().value(), t);
        } catch (Exception e) {
            log.warn("Meta WhatsApp send failed: {}", e.getMessage());
            return WhatsAppSendResult.fail(e.getMessage(), null, true);
        }
    }

    private String bearer() {
        return "Bearer " + accessToken;
    }

    private static String safeId(String providerMessageId) {
        if (providerMessageId == null || providerMessageId.length() < 8) {
            return "(short)";
        }
        return providerMessageId.substring(0, 4) + "…" + providerMessageId.substring(providerMessageId.length() - 4);
    }
}
