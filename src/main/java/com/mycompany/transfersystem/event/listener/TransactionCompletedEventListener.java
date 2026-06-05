package com.mycompany.transfersystem.event.listener;

import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.service.AmlMonitoringService;
import com.mycompany.transfersystem.service.GamificationService;
import com.mycompany.transfersystem.service.ReferralService;
import com.mycompany.transfersystem.service.lending.CreditScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs side effects after a transfer transaction commits: credit scores, trust/gamification, AML, referrals.
 */
@Component
public class TransactionCompletedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionCompletedEventListener.class);

    private final CreditScoringService creditScoringService;
    private final GamificationService gamificationService;
    private final AmlMonitoringService amlMonitoringService;
    private final ReferralService referralService;

    public TransactionCompletedEventListener(CreditScoringService creditScoringService,
                                             GamificationService gamificationService,
                                             AmlMonitoringService amlMonitoringService,
                                             ReferralService referralService) {
        this.creditScoringService = creditScoringService;
        this.gamificationService = gamificationService;
        this.amlMonitoringService = amlMonitoringService;
        this.referralService = referralService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onTransactionCompleted(FinancialWorkflowEvents.TransferCompletedEvent event) {
        if (event.senderId() != null) {
            runSafe("creditScore.sender", () -> creditScoringService.calculateAndSave(event.senderId()));
            runSafe("gamification.sender", () -> gamificationService.refreshTrustScoreForUser(event.senderId()));
            runSafe("referral.sender", () -> referralService.qualifyReferralOnFirstCompletedTransaction(event.senderId()));
        }
        if (event.receiverId() != null) {
            runSafe("creditScore.receiver", () -> creditScoringService.calculateAndSave(event.receiverId()));
            runSafe("gamification.receiver", () -> gamificationService.refreshTrustScoreForUser(event.receiverId()));
            runSafe("referral.receiver", () -> referralService.qualifyReferralOnFirstCompletedTransaction(event.receiverId()));
        }
        runSafe("aml", () -> amlMonitoringService.onTransactionCompleted(event));
    }

    private void runSafe(String step, Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            log.warn("TransactionCompleted follow-up failed [{}]: {}", step, e.getMessage());
        }
    }
}
