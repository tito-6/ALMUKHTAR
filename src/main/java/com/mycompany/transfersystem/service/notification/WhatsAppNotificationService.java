package com.mycompany.transfersystem.service.notification;

import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Facade for WhatsApp-specific domain hooks; delegates to {@link NotificationEventPublisher} and listeners.
 */
@Service
public class WhatsAppNotificationService {

    private final NotificationEventPublisher notificationEventPublisher;

    public WhatsAppNotificationService(NotificationEventPublisher notificationEventPublisher) {
        this.notificationEventPublisher = notificationEventPublisher;
    }

    public void notifyQrReleaseCodeCreated(long transactionId, Instant expiresAt) {
        notificationEventPublisher.publishQrReleaseCodeCreated(transactionId, expiresAt);
    }
}
