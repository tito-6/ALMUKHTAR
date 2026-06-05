package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.FreezeCaseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreezeCaseEventRepository extends JpaRepository<FreezeCaseEvent, Long> {
    List<FreezeCaseEvent> findByFreezeCase_IdOrderByCreatedAtAsc(Long freezeCaseId);
}
