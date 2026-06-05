package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.NotificationOutbox;
import com.mycompany.transfersystem.entity.enums.NotificationOutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    boolean existsByCorrelationId(String correlationId);

    Optional<NotificationOutbox> findByCorrelationId(String correlationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from NotificationOutbox o where o.id = :id")
    Optional<NotificationOutbox> findByIdForUpdate(@Param("id") Long id);

    @Query("select o from NotificationOutbox o where o.status in :statuses and o.nextAttemptAt <= :now order by o.nextAttemptAt asc, o.id asc")
    List<NotificationOutbox> findDueBatch(@Param("statuses") List<NotificationOutboxStatus> statuses,
                                          @Param("now") Instant now,
                                          Pageable pageable);

    long countByStatus(NotificationOutboxStatus status);

    List<NotificationOutbox> findByStatusOrderByCreatedAtDesc(NotificationOutboxStatus status, Pageable pageable);
}
