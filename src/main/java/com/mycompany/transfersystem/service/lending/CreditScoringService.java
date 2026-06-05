package com.mycompany.transfersystem.service.lending;

import com.mycompany.transfersystem.entity.CreditProfile;
import com.mycompany.transfersystem.entity.LoanRepaymentSchedule;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.enums.KycTier;
import com.mycompany.transfersystem.entity.enums.RiskTier;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreditScoringService {

    private static final Logger log = LoggerFactory.getLogger(CreditScoringService.class);
    private static final int ELIGIBILITY_TRUST_SCORE_MIN = 50;
    private static final BigDecimal VOLUME_DIVISOR = new BigDecimal("400");
    private static final int VOLUME_MAX_PTS = 250;
    private static final int FREQUENCY_MULTIPLIER = 4;
    private static final int FREQUENCY_MAX_PTS = 200;
    private static final int TRUST_SCORE_MAX_PTS = 200;
    private static final int REPAYMENT_NEUTRAL_PTS = 100;
    private static final int REPAYMENT_MAX_PTS = 200;
    private static final int MISSED_PENALTY = 40;
    private static final int KYC_BASIC = 20;
    private static final int KYC_STANDARD = 60;
    private static final int KYC_PREMIUM = 100;
    private static final int WALLET_AGE_MULTIPLIER = 5;
    private static final int WALLET_AGE_MAX_PTS = 50;

    private final CreditProfileRepository creditProfileRepository;
    private final TransactionRepository transactionRepository;
    private final TrustScoreRepository trustScoreRepository;
    private final WalletRepository walletRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    public CreditScoringService(CreditProfileRepository creditProfileRepository,
                                TransactionRepository transactionRepository,
                                TrustScoreRepository trustScoreRepository,
                                WalletRepository walletRepository,
                                LoanRepaymentScheduleRepository scheduleRepository,
                                UserRepository userRepository) {
        this.creditProfileRepository = creditProfileRepository;
        this.transactionRepository = transactionRepository;
        this.trustScoreRepository = trustScoreRepository;
        this.walletRepository = walletRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 2 * * MON")
    @SchedulerLock(name = "credit-scoring", lockAtMostFor = "PT45M")
    @Transactional
    public void recalculateAllScores() {
        log.info("Starting weekly credit score recalculation");
        creditProfileRepository.findAll().forEach(cp -> {
            try {
                calculateAndSave(cp.getUser().getId());
            } catch (Exception e) {
                log.warn("Failed to recalculate credit score for user {}: {}", cp.getUser().getId(), e.getMessage());
            }
        });
    }

    @Transactional
    public CreditProfile calculateAndSave(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        CreditProfile profile = creditProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> CreditProfile.builder().user(user).build());
        if (profile.getUser() == null) profile.setUser(user);
        return doCalculateAndSave(profile);
    }

    @Transactional
    public CreditProfile doCalculateAndSave(CreditProfile profile) {
        Long userId = profile.getUser().getId();
        LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);

        BigDecimal totalVolume = transactionRepository.sumAmountByUserSince(userId, twelveMonthsAgo);
        if (totalVolume == null) totalVolume = BigDecimal.ZERO;
        long txCount = transactionRepository.countCompletedByUserSince(userId, twelveMonthsAgo);

        int volPts = totalVolume.compareTo(BigDecimal.ZERO) == 0 ? 0
                : Math.min(VOLUME_MAX_PTS, totalVolume.divide(VOLUME_DIVISOR, 0, RoundingMode.DOWN).intValue());
        int freqPts = (int) Math.min(FREQUENCY_MAX_PTS, txCount * FREQUENCY_MULTIPLIER);

        int trustPts = trustScoreRepository.findByUser_Id(userId)
                .map(ts -> Math.min(TRUST_SCORE_MAX_PTS, Math.min(100, ts.getScore()) * 2))
                .orElse(0);

        List<LoanRepaymentSchedule> allSchedules = scheduleRepository.findSchedulesByUserId(userId);
        int repayPts;
        if (allSchedules.isEmpty()) {
            repayPts = REPAYMENT_NEUTRAL_PTS;
        } else {
            long missed = allSchedules.stream().filter(s -> "MISSED".equals(s.getStatus())).count();
            long paid = allSchedules.stream().filter(s -> "PAID".equals(s.getStatus())).count();
            int total = allSchedules.size();
            repayPts = (int) Math.max(0, (paid * REPAYMENT_MAX_PTS / (total == 0 ? 1 : total)) - (missed * MISSED_PENALTY));
        }

        int kycPts = walletRepository.findByUser_Id(userId)
                .map(w -> {
                    KycTier k = w.getKycTier();
                    if (k == KycTier.PREMIUM) return KYC_PREMIUM;
                    if (k == KycTier.STANDARD) return KYC_STANDARD;
                    return KYC_BASIC;
                })
                .orElse(KYC_BASIC);

        int agePts = walletRepository.findByUser_Id(userId)
                .map(Wallet::getCreatedAt)
                .map(instant -> ChronoUnit.MONTHS.between(instant.atZone(ZoneId.systemDefault()), java.time.ZonedDateTime.now()))
                .map(m -> Math.min(WALLET_AGE_MAX_PTS, (int) (long) m * WALLET_AGE_MULTIPLIER))
                .orElse(0);

        int totalScore = volPts + freqPts + trustPts + repayPts + kycPts + agePts;
        RiskTier tier = scoreToRiskTier(totalScore);
        BigDecimal maxLoanUsd = maxLoanForTier(tier);

        Map<String, Object> components = new HashMap<>();
        components.put("transactionVolumePts", volPts);
        components.put("transactionFrequencyPts", freqPts);
        components.put("trustScorePts", trustPts);
        components.put("repaymentHistoryPts", repayPts);
        components.put("kycTierPts", kycPts);
        components.put("walletAgePts", agePts);

        profile.setCreditScore(totalScore);
        profile.setRiskTier(tier.name());
        profile.setMaxLoanAmountUsd(maxLoanUsd);
        profile.setCalculatedAt(Instant.now());
        profile.setNextReviewAt(Instant.now().plus(7, ChronoUnit.DAYS));
        profile.setScoreComponents(components);
        return creditProfileRepository.save(profile);
    }

    public boolean isEligibleForLoan(Long userId) {
        if (trustScoreRepository.findByUser_Id(userId)
                .map(ts -> ts.getScore() < ELIGIBILITY_TRUST_SCORE_MIN).orElse(true)) {
            return false;
        }
        return creditProfileRepository.findByUser_Id(userId)
                .map(p -> p.getCreditScore() >= 300 && !RiskTier.INELIGIBLE.name().equals(p.getRiskTier()))
                .orElse(false);
    }

    public void reduceScoreByPoints(Long userId, int points) {
        creditProfileRepository.findByUser_Id(userId).ifPresent(profile -> {
            int newScore = Math.max(0, profile.getCreditScore() - points);
            profile.setCreditScore(newScore);
            profile.setRiskTier(scoreToRiskTier(newScore).name());
            profile.setMaxLoanAmountUsd(maxLoanForTier(scoreToRiskTier(newScore)));
            profile.setCalculatedAt(Instant.now());
            creditProfileRepository.save(profile);
        });
    }

    public void addScorePoints(Long userId, int points) {
        creditProfileRepository.findByUser_Id(userId).ifPresent(profile -> {
            int newScore = Math.min(1000, profile.getCreditScore() + points);
            profile.setCreditScore(newScore);
            profile.setRiskTier(scoreToRiskTier(newScore).name());
            profile.setMaxLoanAmountUsd(maxLoanForTier(scoreToRiskTier(newScore)));
            profile.setCalculatedAt(Instant.now());
            creditProfileRepository.save(profile);
        });
    }

    private static RiskTier scoreToRiskTier(int score) {
        if (score >= 700) return RiskTier.TIER_A;
        if (score >= 500) return RiskTier.TIER_B;
        if (score >= 300) return RiskTier.TIER_C;
        return RiskTier.INELIGIBLE;
    }

    private static BigDecimal maxLoanForTier(RiskTier tier) {
        return switch (tier) {
            case TIER_A -> new BigDecimal("5000");
            case TIER_B -> new BigDecimal("2000");
            case TIER_C -> new BigDecimal("500");
            case INELIGIBLE -> BigDecimal.ZERO;
        };
    }
}
