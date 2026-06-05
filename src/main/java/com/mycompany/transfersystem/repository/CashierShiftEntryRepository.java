package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.CashierShiftEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashierShiftEntryRepository extends JpaRepository<CashierShiftEntry, Long> {
    List<CashierShiftEntry> findByShiftIdOrderByCreatedAtAsc(Long shiftId);
}
