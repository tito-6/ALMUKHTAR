package com.mycompany.transfersystem.service.notification.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WhatsAppProvider {

    WhatsAppSendResult sendTemplate(String toE164Phone,
                                    String metaTemplateName,
                                    String languageCode,
                                    List<Map<String, String>> bodyParametersOrdered);

    WhatsAppSendResult sendText(String toE164Phone, String textBody);

    /**
     * Optional media send; implementations may return unsupported without throwing.
     */
    WhatsAppSendResult sendMedia(String toE164Phone, String mediaType, String mediaUrl, String caption);

    Optional<String> getDeliveryStatus(String providerMessageId);
}
