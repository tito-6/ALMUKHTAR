package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.Badge;
import com.mycompany.transfersystem.entity.TrustScore;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.BadgeRepository;
import com.mycompany.transfersystem.repository.TransactionRepository;
import com.mycompany.transfersystem.repository.TrustScoreRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class GamificationService {

    private static final Logger log = LoggerFactory.getLogger(GamificationService.class);
    private static final Map<String, Integer> BADGE_SCORE_BONUSES = Map.of(
            "FIRST_TRANSFER", 10,
            "TEN_TRANSFERS", 25,
            "LOYAL_30_DAYS", 50,
            "HIGH_VOLUME_1K", 75,
            "ELITE_AMBASSADOR", 200
    );

    private final TrustScoreRepository trustScoreRepository;
    private final BadgeRepository badgeRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public GamificationService(TrustScoreRepository trustScoreRepository,
                               BadgeRepository badgeRepository,
                               TransactionRepository transactionRepository,
                               UserRepository userRepository) {
        this.trustScoreRepository = trustScoreRepository;
        this.badgeRepository = badgeRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void recalculateAllScores() {
        log.info("Starting nightly trust score recalculation");
        trustScoreRepository.findAll().forEach(this::recalculateScore);
    }

    /**
     * Recalculate trust score and badges for a user if a {@link TrustScore} row exists.
     */
    @Transactional
    public void refreshTrustScoreForUser(Long userId) {
        if (userId == null) {
            return;
        }
        trustScoreRepository.findByUser_Id(userId).ifPresent(this::recalculateScore);
    }

    @Transactional
    public void recalculateScore(TrustScore trustScore) {
        Long userId = trustScore.getUser().getId();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        long txCount = transactionRepository.countCompletedByUserSince(userId, thirtyDaysAgo);
        BigDecimal vol = transactionRepository.sumAmountByUserSince(userId, thirtyDaysAgo);
        if (vol == null) vol = BigDecimal.ZERO;

        int volumePoints = vol.divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN).intValue();
        int frequencyPoints = (int) (txCount * 3);

        int badgeBonuses = badgeRepository.findByUser_Id(userId).stream()
                .mapToInt(b -> BADGE_SCORE_BONUSES.getOrDefault(b.getBadgeType(), 0))
                .sum();

        int newScore = volumePoints + frequencyPoints + badgeBonuses;
        trustScore.setScore(newScore);
        trustScore.setTier(determineTier(newScore));
        trustScore.setFeeDiscountPct(determineDiscount(newScore));
        trustScore.setUpdatedAt(java.time.Instant.now());
        trustScoreRepository.save(trustScore);

        awardEligibleBadges(userId, txCount, vol);
    }

    @Transactional(readOnly = true)
    public BigDecimal getFeeDiscountMultiplier(Long userId) {
        return trustScoreRepository.findByUser_Id(userId)
                .map(ts -> BigDecimal.ONE.subtract(
                        ts.getFeeDiscountPct().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .orElse(BigDecimal.ONE);
    }

    private void awardEligibleBadges(Long userId, long txCount, BigDecimal vol) {
        if (txCount >= 1) awardBadgeIfNew(userId, "FIRST_TRANSFER");
        if (txCount >= 10) awardBadgeIfNew(userId, "TEN_TRANSFERS");
        if (vol.compareTo(BigDecimal.valueOf(1000)) >= 0) awardBadgeIfNew(userId, "HIGH_VOLUME_1K");
    }

    private void awardBadgeIfNew(Long userId, String badgeType) {
        if (!badgeRepository.existsByUser_IdAndBadgeType(userId, badgeType)) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                badgeRepository.save(Badge.builder()
                        .user(user)
                        .badgeType(badgeType)
                        .awardedAt(java.time.Instant.now())
                        .build());
                log.info("Awarded badge {} to user {}", badgeType, userId);
            }
        }
    }

    /**
     * Award a badge to a user by type (e.g. DEBT_FREE). Idempotent: does not duplicate if already awarded.
     */
    @Transactional
    public void awardBadge(Long userId, String badgeType) {
        awardBadgeIfNew(userId, badgeType);
    }

    private String determineTier(int score) {
        if (score >= 1000) return "ELITE";
        if (score >= 500) return "GOLD";
        if (score >= 200) return "SILVER";
        return "BRONZE";
    }

    private BigDecimal determineDiscount(int score) {
        if (score >= 1000) return BigDecimal.valueOf(15.00);
        if (score >= 500) return BigDecimal.valueOf(10.00);
        if (score >= 200) return BigDecimal.valueOf(5.00);
        return BigDecimal.ZERO;
    }
}
