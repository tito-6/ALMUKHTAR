package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {
    List<InAppNotification> findByUserIdAndDeliveryStatus(Long userId, InAppNotification.DeliveryStatus status);
    Page<InAppNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
