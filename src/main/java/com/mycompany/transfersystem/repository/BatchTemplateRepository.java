package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BatchTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchTemplateRepository extends JpaRepository<BatchTemplate, Long> {

    List<BatchTemplate> findByOwnerUser_Id(Long ownerUserId);
}
