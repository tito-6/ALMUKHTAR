package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BatchJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchJobRepository extends JpaRepository<BatchJob, Long> {

    List<BatchJob> findBySubmittedBy_IdOrderByCreatedAtDesc(Long submittedById);
}
