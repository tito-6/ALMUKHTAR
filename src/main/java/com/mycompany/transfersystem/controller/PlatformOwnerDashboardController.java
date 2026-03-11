package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.service.revenue.RevenueReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/owner")
@CrossOrigin(origins = "*")
public class PlatformOwnerDashboardController {

    private final RevenueReportService revenueReportService;

    public PlatformOwnerDashboardController(RevenueReportService revenueReportService) {
        this.revenueReportService = revenueReportService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(revenueReportService.getDashboardSnapshot());
    }
}
