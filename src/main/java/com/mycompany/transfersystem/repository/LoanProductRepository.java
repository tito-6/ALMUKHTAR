package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    List<LoanProduct> findByRiskTierAndActiveTrue(String riskTier);
    List<LoanProduct> findByActiveTrue();
}
