package com.mycompany.transfersystem.service.revenue;

import com.mycompany.transfersystem.repository.PlatformRevenueEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class RevenueReportService {

    private final PlatformRevenueEntryRepository revenueRepository;

    public RevenueReportService(PlatformRevenueEntryRepository revenueRepository) {
        this.revenueRepository = revenueRepository;
    }

    public Map<String, Object> getDashboardSnapshot() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();
        Instant dayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant weekStart = dayStart.minus(7, ChronoUnit.DAYS);
        Instant monthStart = dayStart.minus(30, ChronoUnit.DAYS);

        BigDecimal todayRevenue = revenueRepository.sumAmountBetween(dayStart, now);
        BigDecimal weekRevenue = revenueRepository.sumAmountBetween(weekStart, now);
        BigDecimal monthRevenue = revenueRepository.sumAmountBetween(monthStart, now);

        Map<String, Object> map = new HashMap<>();
        map.put("todayRevenue", todayRevenue != null ? todayRevenue : BigDecimal.ZERO);
        map.put("weekRevenue", weekRevenue != null ? weekRevenue : BigDecimal.ZERO);
        map.put("monthRevenue", monthRevenue != null ? monthRevenue : BigDecimal.ZERO);
        return map;
    }
}
