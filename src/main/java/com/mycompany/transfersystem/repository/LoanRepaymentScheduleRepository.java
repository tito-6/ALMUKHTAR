package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.LoanRepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, Long> {

    List<LoanRepaymentSchedule> findByLoan_IdOrderByInstalmentNumber(Long loanId);
    List<LoanRepaymentSchedule> findByDueDateAndStatus(LocalDate dueDate, String status);
    List<LoanRepaymentSchedule> findByLoan_IdAndStatus(Long loanId, String status);

    @Query("SELECT s FROM LoanRepaymentSchedule s WHERE s.loan.user.id = :userId")
    List<LoanRepaymentSchedule> findSchedulesByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT s.loan FROM LoanRepaymentSchedule s WHERE s.status = 'MISSED' AND s.dueDate < :before")
    List<com.mycompany.transfersystem.entity.Loan> findLoansWithMissedScheduleBefore(@Param("before") LocalDate before);
}
