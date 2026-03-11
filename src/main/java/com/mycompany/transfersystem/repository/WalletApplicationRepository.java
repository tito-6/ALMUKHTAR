package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.WalletApplication;
import com.mycompany.transfersystem.entity.enums.WalletApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletApplicationRepository extends JpaRepository<WalletApplication, Long> {

    Optional<WalletApplication> findByUser_Id(Long userId);

    List<WalletApplication> findByStatusOrderBySubmittedAtDesc(WalletApplicationStatus status);
}
