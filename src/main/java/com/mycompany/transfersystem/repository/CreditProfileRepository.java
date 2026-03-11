package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.CreditProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditProfileRepository extends JpaRepository<CreditProfile, Long> {

    Optional<CreditProfile> findByUser_Id(Long userId);
}
