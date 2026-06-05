package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.TradableAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradableAssetRepository extends JpaRepository<TradableAsset, Long> {

    List<TradableAsset> findBySymbolIgnoreCaseAndEnabledIsTrue(String symbol);
}
