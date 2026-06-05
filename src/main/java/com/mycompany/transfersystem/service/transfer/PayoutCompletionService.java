package com.mycompany.transfersystem.service.transfer;

import com.mycompany.transfersystem.config.NotificationThresholdProperties;
import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.exception.InvalidTransactionException;
import com.mycompany.transfersystem.repository.TransactionRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.NotificationService;
import com.mycompany.transfersystem.service.liquidity.BranchCashService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single transactional entry point for physical payout completion (QR scan or passcode release).
 * Idempotent when branch cash reservation is already {@code RELEASED}.
 */
@Service
public class PayoutCompletionService {

    public enum ReleaseChannel {
        QR_SCAN,
        PASSCODE
    }

    private final TransactionRepository transactionRepository;
    private final BranchCashService branchCashService;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationThresholdProperties notificationThresholdProperties;
    private final NotificationService notificationService;

    public PayoutCompletionService(TransactionRepository transactionRepository,
                                   BranchCashService branchCashService,
                                   AuditService auditService,
                                   ApplicationEventPublisher applicationEventPublisher,
                                   NotificationThresholdProperties notificationThresholdProperties,
                                   NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.branchCashService = branchCashService;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.notificationThresholdProperties = notificationThresholdProperties;
        this.notificationService = notificationService;
    }

    /**
     * Completes payout after QR path validations (TOTP, token, payload) succeeded elsewhere.
     */
    @Transactional
    public Transaction completeAfterQrScan(Transaction tx, User cashier) {
        return completeInternal(tx.getId(), ReleaseChannel.QR_SCAN, null, null, cashier);
    }

    /**
     * Completes payout after receiver passcode verification.
     */
    @Transactional
    public Transaction completeAfterPasscodeRelease(Long transactionId,
                                                    String passcode,
                                                    Long receiverId,
                                                    User payoutActor) {
        return completeInternal(transactionId, ReleaseChannel.PASSCODE, passcode, receiverId, payoutActor);
    }

    private Transaction completeInternal(Long transactionId,
                                         ReleaseChannel channel,
                                         String passcode,
                                         Long receiverId,
                                         User payoutActor) {
        Transaction tx = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found: " + transactionId));

        if (tx.getStatus() == TransactionStatus.RELEASED) {
            return tx;
        }

        if (channel == ReleaseChannel.PASSCODE) {
            if (tx.getStatus() != TransactionStatus.READY_FOR_PICKUP) {
                throw new InvalidTransactionException("Transaction is not ready for passcode release");
            }
            if (receiverId == null || !tx.getReceiver().getId().equals(receiverId)) {
                throw new InvalidTransactionException("Receiver ID does not match transaction receiver");
            }
            if (tx.getReleasePasscode() == null || !tx.getReleasePasscode().equals(passcode)) {
                throw new InvalidTransactionException("Invalid release passcode");
            }
        } else {
            if (tx.getStatus() != TransactionStatus.PENDING) {
                throw new InvalidTransactionException("Transaction is not pending for QR payout");
            }
        }

        branchCashService.completePayout(tx, payoutActor);

        tx.setStatus(TransactionStatus.RELEASED);
        Transaction saved = transactionRepository.save(tx);

        auditService.log("PAYOUT_RELEASED", payoutActor, "Transaction", saved.getId());

        Long senderBranchId = tx.getSender().getBranch() != null ? tx.getSender().getBranch().getId() : null;
        Long receiverBranchId = tx.getReceiver().getBranch() != null ? tx.getReceiver().getBranch().getId() : null;
        boolean highValue = tx.getAmount() != null
                && tx.getAmount().compareTo(notificationThresholdProperties.getTransferHighValue()) >= 0;

        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TransferReleasedEvent(
                saved.getId(),
                tx.getSender().getId(),
                tx.getReceiver().getId(),
                tx.getAmount(),
                tx.getCurrencyCode() != null ? tx.getCurrencyCode() : "USD",
                java.math.BigDecimal.ZERO,
                senderBranchId,
                receiverBranchId,
                channel.name(),
                payoutActor != null ? payoutActor.getId() : null));

        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TransferCompletedEvent(
                saved.getId(),
                tx.getSender().getId(),
                tx.getReceiver().getId(),
                tx.getAmount(),
                tx.getCurrencyCode() != null ? tx.getCurrencyCode() : "USD",
                java.math.BigDecimal.ZERO,
                senderBranchId,
                receiverBranchId,
                true,
                highValue));

        notificationService.sendEmail(tx.getSender(), "Money Transfer Released",
                "Transaction ID: " + tx.getId() + " was successfully released to the receiver.");

        return saved;
    }
}
