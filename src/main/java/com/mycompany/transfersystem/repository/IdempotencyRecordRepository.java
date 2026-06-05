package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByUserIdAndEndpointAndIdempotencyKey(Long userId, String endpoint, String idempotencyKey);
}
