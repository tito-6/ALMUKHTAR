package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.repository.TransactionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Application-facing entry point for domain notification triggers. Heavy lifting is async after commit.
 */
@Service
public class NotificationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransactionRepository transactionRepository;

    public NotificationEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                                        TransactionRepository transactionRepository) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.transactionRepository = transactionRepository;
    }

    public void publishQrReleaseCodeCreated(long transactionId, Instant expiresAt) {
        Transaction tx = transactionRepository.findById(transactionId).orElse(null);
        if (tx == null || tx.getSender() == null || tx.getReceiver() == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.QrReleaseCreatedEvent(
                transactionId,
                tx.getSender().getId(),
                tx.getReceiver().getId(),
                expiresAt,
                true));
    }
}
