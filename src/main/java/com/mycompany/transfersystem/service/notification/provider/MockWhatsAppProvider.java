package com.mycompany.transfersystem.service.notification.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dev/test provider: never calls the network; stores payloads in-memory for assertions.
 */
public class MockWhatsAppProvider implements WhatsAppProvider {

    public record StoredDelivery(String id,
                                  String toDigits,
                                  String mode,
                                  String templateOrText,
                                  List<Map<String, String>> templateParams) {}

    private final CopyOnWriteArrayList<StoredDelivery> deliveries = new CopyOnWriteArrayList<>();

    @Override
    public WhatsAppSendResult sendTemplate(String toE164Phone,
                                           String metaTemplateName,
                                           String languageCode,
                                           List<Map<String, String>> bodyParametersOrdered) {
        String id = "mock-wamid-" + UUID.randomUUID();
        deliveries.add(new StoredDelivery(id, digits(toE164Phone), "TEMPLATE", metaTemplateName, bodyParametersOrdered));
        return WhatsAppSendResult.ok(id);
    }

    @Override
    public WhatsAppSendResult sendText(String toE164Phone, String textBody) {
        String id = "mock-wamid-" + UUID.randomUUID();
        deliveries.add(new StoredDelivery(id, digits(toE164Phone), "TEXT", textBody, List.of()));
        return WhatsAppSendResult.ok(id);
    }

    @Override
    public WhatsAppSendResult sendMedia(String toE164Phone, String mediaType, String mediaUrl, String caption) {
        String id = "mock-wamid-" + UUID.randomUUID();
        deliveries.add(new StoredDelivery(id, digits(toE164Phone), "MEDIA", mediaType + ":" + mediaUrl, List.of()));
        return WhatsAppSendResult.ok(id);
    }

    @Override
    public Optional<String> getDeliveryStatus(String providerMessageId) {
        return Optional.of("SENT");
    }

    public List<StoredDelivery> getDeliveries() {
        return List.copyOf(deliveries);
    }

    public void clear() {
        deliveries.clear();
    }

    private static String digits(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }
}
