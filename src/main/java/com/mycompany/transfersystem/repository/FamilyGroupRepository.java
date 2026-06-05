package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.FamilyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, Long> {
    List<FamilyGroup> findByOwnerUser_Id(Long ownerId);
}
