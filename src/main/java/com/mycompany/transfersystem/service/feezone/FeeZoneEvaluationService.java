package com.mycompany.transfersystem.service.feezone;

import com.mycompany.transfersystem.entity.FeeZone;
import com.mycompany.transfersystem.repository.FeeZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeeZoneEvaluationService {

    private final FeeZoneRepository feeZoneRepository;
    private final IslamicCalendarService islamicCalendarService;
    private final StringRedisTemplate redisTemplate;

    public BigDecimal getEffectiveDiscount(Long branchId, BigDecimal baseAmount, LocalDate date, String currency) {
        String cacheKey = "feezone:" + branchId + ":" + date;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) return new BigDecimal(cached);
        } catch (Exception e) {
            log.debug("Redis unavailable for fee zone cache");
        }

        BigDecimal discount = computeDiscount(branchId, date);

        try {
            redisTemplate.opsForValue().set(cacheKey, discount.toPlainString(), Duration.ofSeconds(3600));
        } catch (Exception e) {
            log.debug("Redis unavailable, skipping cache write");
        }
        return discount;
    }

    private BigDecimal computeDiscount(Long branchId, LocalDate date) {
        List<FeeZone> zones = feeZoneRepository.findAll();
        BigDecimal highestDiscount = BigDecimal.ZERO;

        for (FeeZone zone : zones) {
            if (!"ACTIVE".equals(zone.getStatus())) continue;
            BigDecimal zoneDiscount = evaluateZoneDiscount(zone, date);
            if (zoneDiscount.compareTo(highestDiscount) > 0) {
                highestDiscount = zoneDiscount;
            }
        }
        return highestDiscount;
    }

    private BigDecimal evaluateZoneDiscount(FeeZone zone, LocalDate date) {
        if (zone.getDescription() != null && zone.getDescription().contains("FEE_FREE")) {
            return new BigDecimal("100");
        }
        if (zone.getDescription() != null && zone.getDescription().contains("ISLAMIC_CALENDAR")) {
            if (islamicCalendarService.isEventActive(
                    com.mycompany.transfersystem.entity.enums.IslamicEvent.RAMADAN, date)) {
                return new BigDecimal("15");
            }
        }
        return BigDecimal.ZERO;
    }
}
