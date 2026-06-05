package com.mycompany.transfersystem.service.savings;

import com.mycompany.transfersystem.dto.savings.CreateSavingsGoalRequest;
import com.mycompany.transfersystem.entity.SavingsGoal;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.event.WalletCreditedEvent;
import com.mycompany.transfersystem.exception.ConditionNotMetException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.SavingsGoalRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final AuditService auditService;

    @Transactional
    public SavingsGoal createGoal(Long userId, CreateSavingsGoalRequest dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        SavingsGoal goal = SavingsGoal.builder()
                .user(user)
                .name(dto.getName())
                .targetAmount(dto.getTargetAmount())
                .currency(dto.getCurrency())
                .deadline(dto.getDeadline())
                .autoSweep(dto.isAutoSweep())
                .build();
        goal = goalRepository.save(goal);
        auditService.log("SAVINGS_GOAL_CREATED", "SAVINGS_GOAL", goal.getId(), null, user);
        return goal;
    }

    @Transactional
    public SavingsGoal contributeManually(Long goalId, BigDecimal amount) {
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found: " + goalId));
        if (goal.getStatus() != SavingsGoal.SavingsGoalStatus.ACTIVE) {
            throw new ConditionNotMetException("Goal is not active");
        }
        goal.setSavedAmount(goal.getSavedAmount().add(amount));
        if (goal.getSavedAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(SavingsGoal.SavingsGoalStatus.ACHIEVED);
        }
        goal = goalRepository.save(goal);
        auditService.log("SAVINGS_CONTRIBUTION", "SAVINGS_GOAL", goal.getId(), "amount=" + amount, goal.getUser());
        return goal;
    }

    @EventListener
    @Async("notificationExecutor")
    @Transactional
    public void onWalletCredited(WalletCreditedEvent event) {
        if (event.userId() == null) return;
        List<SavingsGoal> activeGoals = goalRepository.findActiveByUserId(event.userId());
        if (activeGoals.isEmpty()) return;
        SavingsGoal oldest = activeGoals.stream()
                .filter(SavingsGoal::isAutoSweep)
                .findFirst().orElse(null);
        if (oldest == null) return;
        BigDecimal sweepAmount = event.amount().multiply(new BigDecimal("0.10")).setScale(4, RoundingMode.HALF_UP);
        if (sweepAmount.compareTo(BigDecimal.ZERO) <= 0) return;
        try {
            oldest.setSavedAmount(oldest.getSavedAmount().add(sweepAmount));
            if (oldest.getSavedAmount().compareTo(oldest.getTargetAmount()) >= 0) {
                oldest.setStatus(SavingsGoal.SavingsGoalStatus.ACHIEVED);
            }
            goalRepository.save(oldest);
            log.info("Auto-swept {} to savings goal {} for user {}", sweepAmount, oldest.getId(), event.userId());
        } catch (Exception e) {
            log.warn("Failed to auto-sweep to savings goal {}: {}", oldest.getId(), e.getMessage());
        }
    }

    @Transactional
    public void cancelGoal(Long goalId) {
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found: " + goalId));
        goal.setStatus(SavingsGoal.SavingsGoalStatus.CANCELLED);
        goalRepository.save(goal);
        auditService.log("SAVINGS_GOAL_CANCELLED", "SAVINGS_GOAL", goal.getId(), null, goal.getUser());
    }

    @Transactional(readOnly = true)
    public List<SavingsGoal> getMyGoals(Long userId) {
        return goalRepository.findActiveByUserId(userId);
    }
}
