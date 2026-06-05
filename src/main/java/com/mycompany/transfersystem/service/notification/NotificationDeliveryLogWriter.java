package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.entity.NotificationDeliveryLog;
import com.mycompany.transfersystem.repository.NotificationDeliveryLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeliveryLogWriter {

    private final NotificationDeliveryLogRepository repository;

    public NotificationDeliveryLogWriter(NotificationDeliveryLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDeliveryLog saveNew(NotificationDeliveryLog log) {
        return repository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveUpdate(NotificationDeliveryLog log) {
        repository.save(log);
    }
}
