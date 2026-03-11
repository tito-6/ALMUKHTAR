package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    Optional<PriceSnapshot> findFirstBySymbolOrderByCapturedAtDesc(String symbol);
}
