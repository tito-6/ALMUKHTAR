package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {

    List<LoanRepayment> findByLoan_IdOrderByProcessedAtDesc(Long loanId);
}
