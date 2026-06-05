package com.mycompany.transfersystem.metrics;

import com.mycompany.transfersystem.entity.enums.NotificationOutboxStatus;
import com.mycompany.transfersystem.repository.NotificationOutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationOutboxMetrics {

    public NotificationOutboxMetrics(MeterRegistry registry, NotificationOutboxRepository repository) {
        Gauge.builder("almukhtar_notification_outbox_pending", repository,
                        r -> r.countByStatus(NotificationOutboxStatus.PENDING))
                .description("Notification outbox rows waiting for dispatch")
                .register(registry);
        Gauge.builder("almukhtar_notification_outbox_failed", repository,
                        r -> r.countByStatus(NotificationOutboxStatus.FAILED))
                .description("Notification outbox rows waiting for retry")
                .register(registry);
        Gauge.builder("almukhtar_notification_outbox_dead_letter", repository,
                        r -> r.countByStatus(NotificationOutboxStatus.DEAD_LETTER))
                .description("Notification outbox rows requiring operator action")
                .register(registry);
    }
}
