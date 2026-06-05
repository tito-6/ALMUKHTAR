package com.mycompany.transfersystem.notification;

import com.mycompany.transfersystem.entity.InAppNotification;
import com.mycompany.transfersystem.entity.NotificationDispatchLog;
import com.mycompany.transfersystem.repository.InAppNotificationRepository;
import com.mycompany.transfersystem.repository.NotificationDispatchLogRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.notification.NotificationDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FinancialNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(FinancialNotificationPublisher.class);

    private final NotificationDispatchLogRepository dispatchLogRepository;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final UserRepository userRepository;
    private final NotificationDispatchService notificationDispatchService;

    public FinancialNotificationPublisher(NotificationDispatchLogRepository dispatchLogRepository,
                                          InAppNotificationRepository inAppNotificationRepository,
                                          UserRepository userRepository,
                                          NotificationDispatchService notificationDispatchService) {
        this.dispatchLogRepository = dispatchLogRepository;
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.userRepository = userRepository;
        this.notificationDispatchService = notificationDispatchService;
    }

    /**
     * Each dispatch runs in a new transaction so a failure delivering to one user does not roll back others.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishOne(FinancialNotificationDispatch d, String eventType) {
        if (dispatchLogRepository.existsByIdempotencyKey(d.idempotencyKey())) {
            return;
        }
        try {
            dispatchLogRepository.save(NotificationDispatchLog.builder()
                    .idempotencyKey(d.idempotencyKey())
                    .eventType(eventType)
                    .recipientUserId(d.recipientUserId())
                    .templateKey(d.templateKey())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            return;
        }

        if (d.recipientUserId() == null) {
            return;
        }

        inAppNotificationRepository.save(InAppNotification.builder()
                .userId(d.recipientUserId())
                .title(d.title())
                .body(d.body())
                .type(d.inAppType())
                .deliveryStatus(InAppNotification.DeliveryStatus.DELIVERED)
                .build());

        if (d.sendWhatsApp()) {
            userRepository.findById(d.recipientUserId()).ifPresent(u -> {
                if (u.getPhone() == null || u.getPhone().isBlank()) {
                    return;
                }
                try {
                    notificationDispatchService.dispatchFinancialWhatsapp(
                            u.getId(), u.getPhone(), d.templateKey(), d.title(), d.body());
                } catch (Exception e) {
                    log.warn("WhatsApp delivery skipped for user {}: {}", d.recipientUserId(), e.getMessage());
                }
            });
        }
    }

    public void publishAll(List<FinancialNotificationDispatch> items, String eventType) {
        for (FinancialNotificationDispatch d : items) {
            try {
                publishOne(d, eventType);
            } catch (Exception e) {
                log.warn("Notification dispatch failed for {}: {}", d.idempotencyKey(), e.getMessage());
            }
        }
    }
}
