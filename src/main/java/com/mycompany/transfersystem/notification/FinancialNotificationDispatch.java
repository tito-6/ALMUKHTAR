package com.mycompany.transfersystem.notification;

import com.mycompany.transfersystem.entity.InAppNotification;

public record FinancialNotificationDispatch(
        String idempotencyKey,
        Long recipientUserId,
        String templateKey,
        String title,
        String body,
        InAppNotification.NotificationType inAppType,
        boolean sendWhatsApp
) {
    public FinancialNotificationDispatch(String idempotencyKey,
                                         Long recipientUserId,
                                         String templateKey,
                                         String title,
                                         String body,
                                         InAppNotification.NotificationType inAppType) {
        this(idempotencyKey, recipientUserId, templateKey, title, body, inAppType, true);
    }
}
