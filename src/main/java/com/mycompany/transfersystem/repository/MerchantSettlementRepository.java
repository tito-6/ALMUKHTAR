package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.MerchantSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MerchantSettlementRepository extends JpaRepository<MerchantSettlement, Long> {

    List<MerchantSettlement> findByMerchant_IdOrderByPeriodEndDesc(Long merchantId);
    List<MerchantSettlement> findByStatus(String status);
}
