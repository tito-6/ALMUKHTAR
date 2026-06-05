package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.CashierDrawer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashierDrawerRepository extends JpaRepository<CashierDrawer, Long> {
    Optional<CashierDrawer> findByShiftId(Long shiftId);

    Optional<CashierDrawer> findByShiftIdAndStatus(Long shiftId, CashierDrawer.DrawerStatus status);
}
