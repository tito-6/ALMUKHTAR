package com.mycompany.transfersystem.dto.notification;

import com.mycompany.transfersystem.entity.NotificationOutbox;
import com.mycompany.transfersystem.entity.enums.NotificationOutboxStatus;

import java.time.Instant;

public record NotificationOutboxResponse(
        Long id,
        String eventType,
        String entityType,
        Long entityId,
        Long recipientUserId,
        String recipientRole,
        String channel,
        String phoneMasked,
        String templateKey,
        String languageCode,
        NotificationOutboxStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        String correlationId,
        Instant createdAt,
        Instant processedAt
) {
    public static NotificationOutboxResponse from(NotificationOutbox row) {
        return new NotificationOutboxResponse(
                row.getId(),
                row.getEventType(),
                row.getEntityType(),
                row.getEntityId(),
                row.getRecipientUserId(),
                row.getRecipientRole(),
                row.getChannel(),
                row.getPhoneMasked(),
                row.getTemplateKey(),
                row.getLanguageCode(),
                row.getStatus(),
                row.getAttempts(),
                row.getNextAttemptAt(),
                row.getLastError(),
                row.getCorrelationId(),
                row.getCreatedAt(),
                row.getProcessedAt());
    }
}
