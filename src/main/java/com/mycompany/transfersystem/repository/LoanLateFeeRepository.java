package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.LoanLateFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanLateFeeRepository extends JpaRepository<LoanLateFee, Long> {

    List<LoanLateFee> findByLoan_Id(Long loanId);
}
