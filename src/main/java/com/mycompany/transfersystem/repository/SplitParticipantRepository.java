package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.SplitParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SplitParticipantRepository extends JpaRepository<SplitParticipant, Long> {
    List<SplitParticipant> findBySplitRequestId(Long splitRequestId);
}
