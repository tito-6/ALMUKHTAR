package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.AmlAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmlAlertRepository extends JpaRepository<AmlAlert, Long> {

    List<AmlAlert> findByStatus(String status);
}
