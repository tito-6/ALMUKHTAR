package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.config.properties.AlmukhtarNotificationOutboxProperties;
import com.mycompany.transfersystem.entity.NotificationOutbox;
import com.mycompany.transfersystem.entity.enums.NotificationOutboxStatus;
import com.mycompany.transfersystem.repository.NotificationOutboxRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@ConditionalOnProperty(name = "almukhtar.notifications.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxWorker.class);

    private final AlmukhtarNotificationOutboxProperties properties;
    private final NotificationOutboxRepository repository;
    private final NotificationOutboxProcessor processor;

    public NotificationOutboxWorker(AlmukhtarNotificationOutboxProperties properties,
                                    NotificationOutboxRepository repository,
                                    NotificationOutboxProcessor processor) {
        this.properties = properties;
        this.repository = repository;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${almukhtar.notifications.outbox.fixed-delay-ms:5000}")
    @SchedulerLock(name = "NotificationOutboxWorker", lockAtMostFor = "PT9M", lockAtLeastFor = "PT4S")
    public void tick() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            processBatch();
        } catch (Exception e) {
            log.error("Notification outbox worker tick failed", e);
        }
    }

    /**
     * Exposed for tests; runs one polling batch.
     */
    public void processBatch() {
        List<NotificationOutbox> due = repository.findDueBatch(
                List.of(NotificationOutboxStatus.PENDING, NotificationOutboxStatus.FAILED),
                Instant.now(),
                PageRequest.of(0, Math.max(1, properties.getBatchSize())));
        for (NotificationOutbox row : due) {
            try {
                processor.processRow(row.getId(), properties.getMaxAttempts(), properties.getInitialBackoffSeconds());
            } catch (Exception ex) {
                log.warn("Outbox row {} processing error: {}", row.getId(), ex.getMessage());
            }
        }
    }
}
