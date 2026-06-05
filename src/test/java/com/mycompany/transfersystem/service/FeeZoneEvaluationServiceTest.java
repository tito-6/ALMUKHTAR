package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.FeeZone;
import com.mycompany.transfersystem.repository.FeeZoneRepository;
import com.mycompany.transfersystem.service.feezone.FeeZoneEvaluationService;
import com.mycompany.transfersystem.service.feezone.IslamicCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeZoneEvaluationServiceTest {

    @Mock private FeeZoneRepository feeZoneRepository;
    @Mock private IslamicCalendarService islamicCalendarService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private FeeZoneEvaluationService feeZoneEvaluationService;

    @Test
    void getDiscount_noBranchInZone_returnsZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(feeZoneRepository.findAll()).thenReturn(Collections.emptyList());

        BigDecimal result = feeZoneEvaluationService.getEffectiveDiscount(1L, new BigDecimal("100"), LocalDate.now(), "USD");
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void getDiscount_feeFreeRule_returns100Pct() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        FeeZone zone = FeeZone.builder().status("ACTIVE").description("FEE_FREE zone").build();
        when(feeZoneRepository.findAll()).thenReturn(List.of(zone));

        BigDecimal result = feeZoneEvaluationService.getEffectiveDiscount(1L, new BigDecimal("100"), LocalDate.now(), "USD");
        assertEquals(new BigDecimal("100"), result);
    }

    @Test
    void getDiscount_islamicCalendarRamadan() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        FeeZone zone = FeeZone.builder().status("ACTIVE").description("ISLAMIC_CALENDAR event discount").build();
        when(feeZoneRepository.findAll()).thenReturn(List.of(zone));
        when(islamicCalendarService.isEventActive(any(), any())).thenReturn(true);

        BigDecimal result = feeZoneEvaluationService.getEffectiveDiscount(1L, new BigDecimal("100"), LocalDate.now(), "USD");
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void getDiscount_multipleZones_highestPriorityWins() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        FeeZone zone1 = FeeZone.builder().status("ACTIVE").description("ISLAMIC_CALENDAR event").build();
        FeeZone zone2 = FeeZone.builder().status("ACTIVE").description("FEE_FREE zone").build();
        when(feeZoneRepository.findAll()).thenReturn(List.of(zone1, zone2));

        BigDecimal result = feeZoneEvaluationService.getEffectiveDiscount(1L, new BigDecimal("100"), LocalDate.now(), "USD");
        assertEquals(new BigDecimal("100"), result);
    }
}
