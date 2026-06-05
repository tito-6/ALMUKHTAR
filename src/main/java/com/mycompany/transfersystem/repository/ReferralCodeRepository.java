package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.ReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, Long> {

    Optional<ReferralCode> findByUser_Id(Long userId);

    Optional<ReferralCode> findByCode(String code);
}
