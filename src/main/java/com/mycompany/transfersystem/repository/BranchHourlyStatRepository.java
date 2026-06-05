package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BranchHourlyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface BranchHourlyStatRepository extends JpaRepository<BranchHourlyStat, Long> {
    List<BranchHourlyStat> findByBranchIdAndStatDateBetween(Long branchId, LocalDate from, LocalDate to);
}
