package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.CashTransferOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashTransferOrderRepository extends JpaRepository<CashTransferOrder, Long> {
    List<CashTransferOrder> findByFromBranch_IdOrToBranch_Id(Long fromId, Long toId);
}
