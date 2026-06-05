package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findBySender(User sender);
    List<Transaction> findByReceiver(User receiver);
    List<Transaction> findByStatus(TransactionStatus status);
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.sender = :user OR t.receiver = :user")
    List<Transaction> findByUser(@Param("user") User user);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate, 
                                           Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt > :startDate")
    List<Transaction> findByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt > :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId) AND t.status IN ('COMPLETED','RELEASED') AND t.createdAt > :since")
    long countCompletedByUserSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId) AND t.status IN ('COMPLETED','RELEASED') AND t.createdAt > :since")
    java.math.BigDecimal sumAmountByUserSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE (t.sender.id = :userId OR t.receiver.id = :userId) AND t.status IN ('COMPLETED','RELEASED')")
    long countCompletedByUserAllTime(@Param("userId") Long userId);

    @Query("SELECT DISTINCT t FROM Transaction t JOIN FETCH t.sender JOIN FETCH t.receiver JOIN FETCH t.fund WHERE t.id = :id")
    Optional<Transaction> findByIdWithParticipants(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") Long id);
}