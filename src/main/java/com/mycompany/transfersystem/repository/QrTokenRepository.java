package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.QrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface QrTokenRepository extends JpaRepository<QrToken, Long> {

    Optional<QrToken> findByTokenHashAndUsedFalseAndExpiresAtAfter(
            String tokenHash, Instant now);

    @Query("SELECT q FROM QrToken q WHERE q.transaction.id = :txId AND q.used = false")
    Optional<QrToken> findActiveByTransactionId(@Param("txId") Long txId);

    @Modifying
    @Query("UPDATE QrToken q SET q.used = true WHERE q.expiresAt < :now AND q.used = false")
    int expireStaleTokens(@Param("now") Instant now);
}
