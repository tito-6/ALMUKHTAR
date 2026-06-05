package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByMemberUserIdAndActiveTrue(Long memberUserId);
    List<FamilyMember> findByFamilyGroupId(Long familyGroupId);
    Optional<FamilyMember> findByFamilyGroupIdAndMemberUserId(Long groupId, Long memberUserId);
}
