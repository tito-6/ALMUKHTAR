package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.SyncQueue;
import com.mycompany.transfersystem.entity.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncQueueRepository extends JpaRepository<SyncQueue, Long> {
    List<SyncQueue> findByDeviceIdAndStatusOrderByOfflineTimestampAsc(String deviceId, SyncStatus status);
    boolean existsByPayloadChecksumAndStatus(String payloadChecksum, SyncStatus status);
}
