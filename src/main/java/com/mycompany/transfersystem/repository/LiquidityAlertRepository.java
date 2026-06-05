package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.LiquidityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LiquidityAlertRepository extends JpaRepository<LiquidityAlert, Long> {
    List<LiquidityAlert> findByBranchIdAndResolvedFalse(Long branchId);
    List<LiquidityAlert> findByResolvedFalse();
    Optional<LiquidityAlert> findByBranchIdAndAlertTypeAndCurrencyAndResolvedFalse(
            Long branchId, LiquidityAlert.AlertType alertType, String currency);
}
