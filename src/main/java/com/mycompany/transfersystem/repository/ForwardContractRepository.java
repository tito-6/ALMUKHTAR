package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.ForwardContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForwardContractRepository extends JpaRepository<ForwardContract, Long> {

    List<ForwardContract> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<ForwardContract> findByStatus(String status);
}
