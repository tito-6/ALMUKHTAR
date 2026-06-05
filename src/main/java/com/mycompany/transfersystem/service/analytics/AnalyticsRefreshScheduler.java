package com.mycompany.transfersystem.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyticsRefreshScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 */15 * * * *")
    @SchedulerLock(name = "analytics-refresh", lockAtMostFor = "PT2M")
    public void refreshViews() {
        try {
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY branch_hourly_stats_view");
            log.info("Analytics materialized view refreshed");
        } catch (Exception e) {
            log.error("Failed to refresh analytics materialized view: {}", e.getMessage());
        }
    }
}
