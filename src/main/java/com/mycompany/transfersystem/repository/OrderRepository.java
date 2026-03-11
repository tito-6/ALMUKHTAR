package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Order;
import com.mycompany.transfersystem.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByTradingAccount_IdOrderByCreatedAtDesc(Long tradingAccountId, Pageable pageable);

    List<Order> findByTradingAccount_IdAndStatus(Long tradingAccountId, OrderStatus status);
}
