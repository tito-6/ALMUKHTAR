package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.EscrowContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface EscrowContractRepository extends JpaRepository<EscrowContract, Long> {

    List<EscrowContract> findByInitiatorUser_IdOrBeneficiaryUser_Id(Long initiatorId, Long beneficiaryId);
    List<EscrowContract> findByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EscrowContract e WHERE e.id = :id")
    Optional<EscrowContract> findByIdForUpdate(@Param("id") Long id);
}
