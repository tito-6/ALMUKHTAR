package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long> {

    Optional<TradingAccount> findByUser_Id(Long userId);
}
