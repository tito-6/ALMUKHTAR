package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<LoanApplication> findByStatusOrderByCreatedAtAsc(String status);
}
