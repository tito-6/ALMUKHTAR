package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.BranchIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchIncidentRepository extends JpaRepository<BranchIncident, Long> {
    List<BranchIncident> findByBranch_IdAndActiveTrue(Long branchId);

    Optional<BranchIncident> findFirstByBranch_IdAndActiveTrueOrderByStartedAtDesc(Long branchId);
}
