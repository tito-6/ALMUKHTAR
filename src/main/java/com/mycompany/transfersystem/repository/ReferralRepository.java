package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    Optional<Referral> findByReferred_IdAndStatus(Long referredUserId, String status);
}
