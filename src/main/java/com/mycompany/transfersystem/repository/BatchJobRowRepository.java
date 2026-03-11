package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BatchJobRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchJobRowRepository extends JpaRepository<BatchJobRow, Long> {

    List<BatchJobRow> findByBatchJob_IdOrderByRowNumber(Long batchJobId);
}
