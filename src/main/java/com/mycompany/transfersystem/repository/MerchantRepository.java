package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByOwnerUser_Id(Long ownerUserId);
    List<Merchant> findByStatus(String status);
}
