package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUserIdAndStatus(Long userId, SavingsGoal.SavingsGoalStatus status);

    @Query("SELECT g FROM SavingsGoal g WHERE g.user.id = :userId AND g.status = 'ACTIVE' ORDER BY g.createdAt ASC")
    List<SavingsGoal> findActiveByUserId(Long userId);
}
