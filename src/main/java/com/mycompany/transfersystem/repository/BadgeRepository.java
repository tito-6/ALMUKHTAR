package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {
    List<Badge> findByUser_Id(Long userId);
    boolean existsByUser_IdAndBadgeType(Long userId, String badgeType);
}
