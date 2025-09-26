package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundRepository extends JpaRepository<Fund, Long> {
    List<Fund> findByStatus(FundStatus status);
    boolean existsByName(String name);
}