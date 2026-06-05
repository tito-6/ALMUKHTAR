package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.AccountFreezeCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountFreezeCaseRepository extends JpaRepository<AccountFreezeCase, Long> {
    Optional<AccountFreezeCase> findByPublicCaseId(String publicCaseId);

    List<AccountFreezeCase> findByUser_IdAndStatusIn(Long userId, List<AccountFreezeCase.CaseStatus> statuses);
}
