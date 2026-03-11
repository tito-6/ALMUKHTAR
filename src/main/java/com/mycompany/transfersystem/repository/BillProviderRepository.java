package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BillProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillProviderRepository extends JpaRepository<BillProvider, Long> {

    List<BillProvider> findByActiveTrue();
    List<BillProvider> findByCategoryAndActiveTrue(String category);
}
