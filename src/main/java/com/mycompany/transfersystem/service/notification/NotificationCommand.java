package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.entity.enums.NotificationEventType;

import java.util.Map;

/**
 * Command describing one outbound notification to enqueue.
 */
public record NotificationCommand(
        String eventType,
        String entityType,
        Long entityId,
        Long recipientUserId,
        String recipientRole,
        String channel,
        String phoneMasked,
        String recipientPhoneE164,
        String templateKey,
        String languageCode,
        Map<String, String> payloadVariables,
        NotificationEventType structuredEventType
) {
    public static String correlationId(String eventType,
                                       String entityType,
                                       Long entityId,
                                       Long recipientUserId,
                                       String channel,
                                       String templateKey) {
        String rid = recipientUserId == null ? "null" : String.valueOf(recipientUserId);
        String eid = entityId == null ? "null" : String.valueOf(entityId);
        return com.mycompany.transfersystem.service.idempotency.IdempotencyService.sha256Hex(
                eventType + "|" + entityType + "|" + eid + "|" + rid + "|" + channel + "|" + templateKey);
    }
}
