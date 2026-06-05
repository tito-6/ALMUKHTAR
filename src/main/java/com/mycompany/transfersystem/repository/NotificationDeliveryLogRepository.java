package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.NotificationDeliveryLog;
import com.mycompany.transfersystem.entity.enums.NotificationDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, Long> {

    Optional<NotificationDeliveryLog> findByProviderMessageId(String providerMessageId);

    List<NotificationDeliveryLog> findByStatusInOrderByCreatedAtDesc(
            List<NotificationDeliveryStatus> statuses);
}
