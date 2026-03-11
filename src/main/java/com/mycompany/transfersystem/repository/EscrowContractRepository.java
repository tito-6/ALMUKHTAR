package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.EscrowContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscrowContractRepository extends JpaRepository<EscrowContract, Long> {

    List<EscrowContract> findByInitiatorUser_IdOrBeneficiaryUser_Id(Long initiatorId, Long beneficiaryId);
    List<EscrowContract> findByStatus(String status);
}
