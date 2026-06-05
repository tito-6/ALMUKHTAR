package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.Referral;
import com.mycompany.transfersystem.repository.ReferralCodeRepository;
import com.mycompany.transfersystem.repository.ReferralRepository;
import com.mycompany.transfersystem.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Qualifies pending referrals when a user completes their first transaction.
 */
@Service
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);

    private final ReferralRepository referralRepository;
    private final ReferralCodeRepository referralCodeRepository;
    private final TransactionRepository transactionRepository;

    public ReferralService(ReferralRepository referralRepository,
                           ReferralCodeRepository referralCodeRepository,
                           TransactionRepository transactionRepository) {
        this.referralRepository = referralRepository;
        this.referralCodeRepository = referralCodeRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * If {@code userId} has exactly one completed transaction (lifetime), move matching PENDING referral to QUALIFIED
     * and bump referrer's {@code total_referrals}.
     */
    @Transactional
    public void qualifyReferralOnFirstCompletedTransaction(Long userId) {
        if (userId == null) {
            return;
        }
        long completed = transactionRepository.countCompletedByUserAllTime(userId);
        if (completed != 1) {
            return;
        }
        referralRepository.findByReferred_IdAndStatus(userId, "PENDING").ifPresent(ref -> {
            ref.setStatus("QUALIFIED");
            referralRepository.save(ref);
            Long referrerId = ref.getReferrer().getId();
            referralCodeRepository.findByUser_Id(referrerId).ifPresent(rc -> {
                rc.setTotalReferrals(rc.getTotalReferrals() + 1);
                referralCodeRepository.save(rc);
            });
            log.info("Referral {} qualified for referred user {}", ref.getId(), userId);
        });
    }

    @Transactional(readOnly = true)
    public Optional<Referral> findPendingForReferred(Long referredUserId) {
        return referralRepository.findByReferred_IdAndStatus(referredUserId, "PENDING");
    }
}
