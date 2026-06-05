package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.NotificationDispatchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDispatchLogRepository extends JpaRepository<NotificationDispatchLog, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
