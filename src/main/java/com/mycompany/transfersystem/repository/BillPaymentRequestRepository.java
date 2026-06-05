package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BillPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillPaymentRequestRepository extends JpaRepository<BillPaymentRequest, Long> {

    List<BillPaymentRequest> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<BillPaymentRequest> findByStatus(String status);

    long countByStatus(String status);
}
