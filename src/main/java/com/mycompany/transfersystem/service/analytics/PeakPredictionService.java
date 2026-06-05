package com.mycompany.transfersystem.service.analytics;

import com.mycompany.transfersystem.entity.BranchHourlyStat;
import com.mycompany.transfersystem.repository.BranchHourlyStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PeakPredictionService {

    private final BranchHourlyStatRepository statRepository;

    public Map<DayOfWeek, List<Integer>> predictPeakHoursNextWeek(Long branchId) {
        LocalDate fourWeeksAgo = LocalDate.now().minusWeeks(4);
        List<BranchHourlyStat> stats = statRepository.findByBranchIdAndStatDateBetween(branchId, fourWeeksAgo, LocalDate.now());
        Map<DayOfWeek, List<Integer>> predictions = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, Map<Integer, Double>> byDayAndHour = stats.stream()
                .collect(Collectors.groupingBy(s -> s.getStatDate().getDayOfWeek(),
                        Collectors.groupingBy(BranchHourlyStat::getHour,
                                Collectors.averagingInt(BranchHourlyStat::getTransactionCount))));
        for (var entry : byDayAndHour.entrySet()) {
            List<Integer> peakHours = entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .toList();
            predictions.put(entry.getKey(), peakHours);
        }
        return predictions;
    }
}
