package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
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
}