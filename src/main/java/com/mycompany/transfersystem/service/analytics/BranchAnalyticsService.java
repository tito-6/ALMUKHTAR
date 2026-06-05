package com.mycompany.transfersystem.service.analytics;

import com.mycompany.transfersystem.entity.BranchHourlyStat;
import com.mycompany.transfersystem.repository.BranchHourlyStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchAnalyticsService {

    private final BranchHourlyStatRepository statRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDailyReport(Long branchId, LocalDate date) {
        List<BranchHourlyStat> stats = statRepository.findByBranchIdAndStatDateBetween(branchId, date, date);
        int totalTx = stats.stream().mapToInt(BranchHourlyStat::getTransactionCount).sum();
        BigDecimal totalVol = stats.stream().map(BranchHourlyStat::getTotalVolume)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> report = new HashMap<>();
        report.put("branchId", branchId);
        report.put("date", date);
        report.put("totalTransactions", totalTx);
        report.put("totalVolume", totalVol);
        report.put("hourlyBreakdown", stats);
        return report;
    }

    @Transactional(readOnly = true)
    public List<Integer> getPeakHours(Long branchId, LocalDate from, LocalDate to) {
        List<BranchHourlyStat> stats = statRepository.findByBranchIdAndStatDateBetween(branchId, from, to);
        return stats.stream()
                .collect(Collectors.groupingBy(BranchHourlyStat::getHour,
                        Collectors.averagingInt(BranchHourlyStat::getTransactionCount)))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformOverview(LocalDate from, LocalDate to) {
        List<BranchHourlyStat> all = statRepository.findByBranchIdAndStatDateBetween(null, from, to);
        if (all == null) all = statRepository.findAll();
        Map<String, Object> overview = new HashMap<>();
        overview.put("from", from);
        overview.put("to", to);
        overview.put("totalTransactions", all.stream().mapToInt(BranchHourlyStat::getTransactionCount).sum());
        return overview;
    }
}
