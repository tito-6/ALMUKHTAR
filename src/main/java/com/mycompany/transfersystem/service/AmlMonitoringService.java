package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.AmlAlert;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.repository.AmlAlertRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lightweight AML hooks driven by completed transactions (alerts only; no blocking).
 */
@Service
public class AmlMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(AmlMonitoringService.class);
    /** Rule id for high single-tx amount (seed-level stub). */
    private static final long RULE_HIGH_VALUE_SINGLE_TX = 1L;
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("50000");

    private final AmlAlertRepository amlAlertRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AmlMonitoringService(AmlAlertRepository amlAlertRepository,
                                UserRepository userRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.amlAlertRepository = amlAlertRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void onTransactionCompleted(FinancialWorkflowEvents.TransferCompletedEvent event) {
        if (event.amount() == null || event.senderId() == null) {
            return;
        }
        if (event.amount().compareTo(HIGH_VALUE_THRESHOLD) < 0) {
            return;
        }
        User sender = userRepository.findById(event.senderId()).orElse(null);
        if (sender == null) {
            return;
        }
        AmlAlert alert = AmlAlert.builder()
                .user(sender)
                .ruleId(RULE_HIGH_VALUE_SINGLE_TX)
                .triggeredTransactions(List.of(event.transactionId()))
                .severity("MEDIUM")
                .status("OPEN")
                .notes("High-value completed transaction threshold")
                .build();
        alert = amlAlertRepository.save(alert);
        log.info("AML alert created for user {} transaction {}", sender.getId(), event.transactionId());
        eventPublisher.publishEvent(new FinancialWorkflowEvents.AmlAlertCreatedEvent(
                alert.getId(),
                sender.getId(),
                alert.getSeverity(),
                "High-value completed transaction threshold"));
    }
}
