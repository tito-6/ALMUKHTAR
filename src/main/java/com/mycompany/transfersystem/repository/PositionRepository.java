package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByTradingAccount_Id(Long tradingAccountId);

    Optional<Position> findByTradingAccount_IdAndSymbol(Long tradingAccountId, String symbol);
}
