package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUser_IdOrderByDisbursedAtDesc(Long userId);
    List<Loan> findByStatus(String status);
}
