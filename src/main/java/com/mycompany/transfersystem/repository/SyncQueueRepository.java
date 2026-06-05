package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.SyncQueue;
import com.mycompany.transfersystem.entity.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SyncQueueRepository extends JpaRepository<SyncQueue, Long> {
    List<SyncQueue> findByDeviceIdAndStatusOrderByOfflineTimestampAsc(String deviceId, SyncStatus status);
    boolean existsByPayloadChecksumAndStatus(String payloadChecksum, SyncStatus status);
    Optional<SyncQueue> findByDeviceIdAndIdempotencyKey(String deviceId, String idempotencyKey);
    boolean existsByDeviceIdAndIdempotencyKeyAndStatusAndIdNot(String deviceId, String idempotencyKey, SyncStatus status, Long id);

    @Query("""
            select coalesce(max(s.deviceSequenceNumber), -1) from SyncQueue s
            where coalesce(s.cashierDeviceId, s.deviceId) = :deviceId
              and s.status = :applied
            """)
    Optional<Long> findMaxAppliedDeviceSequence(@Param("deviceId") String deviceId, @Param("applied") SyncStatus applied);

    @Query("""
            select count(s) from SyncQueue s
            where s.cashier.id = :cashierId and s.status = :status
              and s.syncedAt >= :since
            """)
    long countAppliedSince(@Param("cashierId") Long cashierId,
                           @Param("status") SyncStatus status,
                           @Param("since") Instant since);
}
