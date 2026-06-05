package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.TradingRiskProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradingRiskProfileRepository extends JpaRepository<TradingRiskProfile, Long> {

    Optional<TradingRiskProfile> findByUser_Id(Long userId);
}
