package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.PlatformTradingFee;
import com.mycompany.transfersystem.entity.enums.AssetClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PlatformTradingFeeRepository extends JpaRepository<PlatformTradingFee, Long> {

    Optional<PlatformTradingFee> findFirstByAssetClassAndEffectiveFromBeforeOrderByEffectiveFromDesc(
            AssetClass assetClass, Instant asOf);
}
