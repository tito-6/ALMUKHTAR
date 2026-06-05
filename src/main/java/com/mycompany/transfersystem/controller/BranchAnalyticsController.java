package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.service.analytics.BranchAnalyticsService;
import com.mycompany.transfersystem.service.analytics.PeakPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BranchAnalyticsController {

    private final BranchAnalyticsService branchAnalyticsService;
    private final PeakPredictionService peakPredictionService;

    @GetMapping("/my-branch/daily")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> daily(@RequestParam Long branchId, @RequestParam String date) {
        return ResponseEntity.ok(branchAnalyticsService.getDailyReport(branchId, LocalDate.parse(date)));
    }

    @GetMapping("/my-branch/peak-hours")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<List<Integer>> peakHours(@RequestParam Long branchId,
                                                     @RequestParam String from, @RequestParam String to) {
        return ResponseEntity.ok(branchAnalyticsService.getPeakHours(branchId, LocalDate.parse(from), LocalDate.parse(to)));
    }

    @GetMapping("/platform")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<Map<String, Object>> platform(@RequestParam String from, @RequestParam String to) {
        return ResponseEntity.ok(branchAnalyticsService.getPlatformOverview(LocalDate.parse(from), LocalDate.parse(to)));
    }
}
