package com.mycompany.transfersystem.service.liquidity;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.BranchCashInventory;
import com.mycompany.transfersystem.entity.BranchCashMovement;
import com.mycompany.transfersystem.entity.BranchVaultBalance;
import com.mycompany.transfersystem.entity.LiquidityAlert;
import com.mycompany.transfersystem.entity.PayoutReservation;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.BranchCashInventoryRepository;
import com.mycompany.transfersystem.repository.BranchCashMovementRepository;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.BranchVaultBalanceRepository;
import com.mycompany.transfersystem.repository.LiquidityAlertRepository;
import com.mycompany.transfersystem.repository.PayoutReservationRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class LiquidityForecastService {

    private final LiquidityAlertRepository alertRepository;
    private final BranchCashInventoryRepository inventoryRepository;
    private final BranchCashMovementRepository movementRepository;
    private final PayoutReservationRepository payoutReservationRepository;
    private final BranchVaultBalanceRepository vaultBalanceRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final int FORECAST_LOOKBACK_DAYS = 30;
    private static final int HOURS_48 = 48;
    private static final int HOURS_168 = 168;

    @Scheduled(cron = "0 0 */4 * * *")
    @SchedulerLock(name = "liquidity-monitor", lockAtMostFor = "PT10M")
    @Transactional
    public void checkAll() {
        try {
            log.info("Running liquidity check for all branches");
            int created = 0;
            for (BranchCashInventory inventory : inventoryRepository.findAll()) {
                if (inventory.getLowCashThreshold() != null
                        && inventory.getLowCashThreshold().compareTo(BigDecimal.ZERO) > 0
                        && inventory.getAvailableBalance().compareTo(inventory.getLowCashThreshold()) <= 0) {
                    created += createLowCashAlertIfNeeded(inventory);
                }
            }
            log.info("Liquidity check completed; created {} alerts", created);
        } catch (Exception e) {
            log.error("Error during liquidity check", e);
        }
    }

    @Scheduled(cron = "0 15 */6 * * *")
    @SchedulerLock(name = "liquidity-shortage-forecast", lockAtMostFor = "PT10M")
    @Transactional
    public void forecastShortagesAndNotify() {
        try {
            for (BranchCashInventory inv : inventoryRepository.findAll()) {
                Map<String, Object> row = forecastCurrency(inv);
                Boolean shortage48 = (Boolean) row.get("shortagePredicted48h");
                Boolean shortage7d = (Boolean) row.get("shortagePredicted7d");
                if (Boolean.TRUE.equals(shortage48) || Boolean.TRUE.equals(shortage7d)) {
                    if (createForecastShortageAlertIfNeeded(inv, row)) {
                        String msg = "Shortage risk " + inv.getCurrency() + " branch " + inv.getBranch().getId()
                                + " conf=" + row.get("confidenceScore");
                        notificationService.notifyBranchOperation(inv.getBranch().getId(), "Liquidity forecast", msg);
                        notificationService.notifyUsersByRole(UserRole.MOTHER_BRANCH_ADMIN,
                                "Liquidity forecast", msg);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Shortage forecast job failed", e);
        }
    }

    @Transactional
    public void resolveAlert(Long alertId, Long resolverId) {
        LiquidityAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));
        alert.setResolved(true);
        alertRepository.save(alert);
        var resolver = userRepository.findById(resolverId).orElse(null);
        auditService.log("LIQUIDITY_ALERT_RESOLVED", "LIQUIDITY_ALERT", alertId, null, resolver);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getForecast(Long branchId) {
        Map<String, Object> forecast = new LinkedHashMap<>();
        forecast.put("branchId", branchId);
        forecast.put("lookbackDays", FORECAST_LOOKBACK_DAYS);
        forecast.put("horizon48Hours", HOURS_48);
        forecast.put("horizon7DaysHours", HOURS_168);
        Branch branch = branchRepository.findById(branchId).orElse(null);
        forecast.put("branchHours", branchHours(branch));

        List<Map<String, Object>> currencies = inventoryRepository.findByBranchId(branchId).stream()
                .map(this::forecastCurrency)
                .toList();
        forecast.put("currencies", currencies);
        forecast.put("note", "Forecast uses vault, inventory, payout reservations, historical cash-in/out, day-of-week factor, and branch hours.");
        return forecast;
    }

    @Transactional(readOnly = true)
    public List<LiquidityAlert> getUnresolvedAlerts() {
        return alertRepository.findByResolvedFalse();
    }

    private Map<String, Object> branchHours(Branch branch) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (branch == null) {
            return m;
        }
        m.put("opensAt", branch.getOpensAt() != null ? branch.getOpensAt().toString() : null);
        m.put("closesAt", branch.getClosesAt() != null ? branch.getClosesAt().toString() : null);
        return m;
    }

    private int branchWeeklyOpenHours(Branch branch) {
        if (branch == null || branch.getOpensAt() == null || branch.getClosesAt() == null) {
            return 84;
        }
        LocalTime o = branch.getOpensAt();
        LocalTime c = branch.getClosesAt();
        int daily = c.toSecondOfDay() - o.toSecondOfDay();
        if (daily <= 0) {
            return 84;
        }
        return Math.max(1, (int) Math.round(daily / 3600.0)) * 7;
    }

    private Map<String, Object> forecastCurrency(BranchCashInventory inventory) {
        LocalDateTime since = LocalDateTime.now().minusDays(FORECAST_LOOKBACK_DAYS);
        List<BranchCashMovement> movements = movementRepository
                .findByBranchIdAndCurrencyAndCreatedAtAfter(inventory.getBranch().getId(), inventory.getCurrency(), since);

        BigDecimal totalReleased = movements.stream()
                .filter(m -> m.getMovementType() == BranchCashMovement.MovementType.TRANSFER_PAYOUT_RELEASED)
                .map(BranchCashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCashIn = movements.stream()
                .filter(m -> m.getMovementType() == BranchCashMovement.MovementType.CASH_IN_RECEIVED)
                .map(BranchCashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageDailyOutflow = totalReleased.divide(BigDecimal.valueOf(FORECAST_LOOKBACK_DAYS), 4, RoundingMode.HALF_UP);
        BigDecimal averageDailyInflow = totalCashIn.divide(BigDecimal.valueOf(FORECAST_LOOKBACK_DAYS), 4, RoundingMode.HALF_UP);

        DayOfWeek dow = LocalDateTime.now().getDayOfWeek();
        BigDecimal dowFactor = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY)
                ? new BigDecimal("1.12") : BigDecimal.ONE;

        BigDecimal payrollBump = new BigDecimal("1.0");
        if (dow == DayOfWeek.THURSDAY || dow == DayOfWeek.FRIDAY) {
            payrollBump = new BigDecimal("1.08");
        }

        BigDecimal projected48hOutflow = averageDailyOutflow
                .multiply(BigDecimal.valueOf(2))
                .multiply(dowFactor)
                .multiply(payrollBump)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal projected7dOutflow = averageDailyOutflow
                .multiply(BigDecimal.valueOf(7))
                .multiply(dowFactor)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal reservedPickups = payoutReservationRepository.sumActiveForBranchCurrency(
                inventory.getBranch().getId(), inventory.getCurrency(), PayoutReservation.ReservationStatus.ACTIVE);

        BigDecimal vault = vaultBalanceRepository.findByBranch_IdAndCurrency(inventory.getBranch().getId(), inventory.getCurrency())
                .map(BranchVaultBalance::getVaultBalance)
                .orElse(inventory.getAvailableBalance());

        BigDecimal netDaily = averageDailyInflow.subtract(averageDailyOutflow);
        BigDecimal projectedBalance48h = inventory.getAvailableBalance()
                .subtract(inventory.getReservedBalance())
                .subtract(reservedPickups)
                .subtract(projected48hOutflow)
                .add(netDaily.multiply(BigDecimal.valueOf(2)))
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal projectedBalance7d = inventory.getAvailableBalance()
                .subtract(inventory.getReservedBalance())
                .subtract(reservedPickups)
                .subtract(projected7dOutflow)
                .add(netDaily.multiply(BigDecimal.valueOf(7)))
                .setScale(4, RoundingMode.HALF_UP);

        Branch br = inventory.getBranch();
        int weeklyOpenH = branchWeeklyOpenHours(br);
        BigDecimal hourlyOut = weeklyOpenH > 0
                ? averageDailyOutflow.multiply(BigDecimal.valueOf(7)).divide(BigDecimal.valueOf(weeklyOpenH), 6, RoundingMode.HALF_UP)
                : averageDailyOutflow.divide(BigDecimal.valueOf(24), 6, RoundingMode.HALF_UP);

        BigDecimal freeNow = inventory.getAvailableBalance().subtract(inventory.getReservedBalance()).subtract(reservedPickups);
        BigDecimal hoursToShortage = hourlyOut.compareTo(BigDecimal.ZERO) <= 0
                ? null
                : freeNow.divide(hourlyOut, 2, RoundingMode.HALF_UP);

        double dataPoints = movements.size();
        double confidence = Math.min(0.95, 0.35 + dataPoints / 200.0);

        String recommended;
        if (projectedBalance48h.compareTo(inventory.getLowCashThreshold()) <= 0) {
            recommended = "RESTOCK_WITHIN_48H";
        } else if (projectedBalance7d.compareTo(inventory.getLowCashThreshold()) <= 0) {
            recommended = "SCHEDULE_INTER_BRANCH_TRANSFER_THIS_WEEK";
        } else {
            recommended = "MONITOR";
        }

        boolean shortage48 = projectedBalance48h.compareTo(inventory.getLowCashThreshold()) <= 0
                && inventory.getLowCashThreshold().compareTo(BigDecimal.ZERO) > 0;
        boolean shortage7d = projectedBalance7d.compareTo(inventory.getLowCashThreshold()) <= 0
                && inventory.getLowCashThreshold().compareTo(BigDecimal.ZERO) > 0;

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("currency", inventory.getCurrency());
        row.put("availableBalance", inventory.getAvailableBalance());
        row.put("reservedBalance", inventory.getReservedBalance());
        row.put("reservedPickupReservations", reservedPickups);
        row.put("vaultBalanceSnapshot", vault);
        row.put("averageDailyOutflow", averageDailyOutflow);
        row.put("averageDailyInflow", averageDailyInflow);
        row.put("dayOfWeekFactor", dowFactor);
        row.put("payrollCycleFactor", payrollBump);
        row.put("projected48hOutflow", projected48hOutflow);
        row.put("projected7dOutflow", projected7dOutflow);
        row.put("projectedBalance48h", projectedBalance48h);
        row.put("projectedBalance7d", projectedBalance7d);
        row.put("hoursToShortageEstimate", hoursToShortage);
        row.put("lowCashThreshold", inventory.getLowCashThreshold());
        row.put("confidenceScore", BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP));
        row.put("recommendedAction", recommended);
        row.put("shortagePredicted48h", shortage48);
        row.put("shortagePredicted7d", shortage7d);
        return row;
    }

    private int createLowCashAlertIfNeeded(BranchCashInventory inventory) {
        boolean exists = alertRepository.findByBranchIdAndAlertTypeAndCurrencyAndResolvedFalse(
                inventory.getBranch().getId(), LiquidityAlert.AlertType.LOW_CASH, inventory.getCurrency()).isPresent();
        if (exists) {
            return 0;
        }
        alertRepository.save(LiquidityAlert.builder()
                .branchId(inventory.getBranch().getId())
                .alertType(LiquidityAlert.AlertType.LOW_CASH)
                .currency(inventory.getCurrency())
                .currentBalance(inventory.getAvailableBalance())
                .thresholdBreached(inventory.getLowCashThreshold())
                .message("Low cash at branch " + inventory.getBranch().getName()
                        + " for " + inventory.getCurrency() + ". Available "
                        + inventory.getAvailableBalance() + ", threshold " + inventory.getLowCashThreshold())
                .resolved(false)
                .build());
        return 1;
    }

    private boolean createForecastShortageAlertIfNeeded(BranchCashInventory inventory, Map<String, Object> forecastRow) {
        boolean exists = alertRepository.findByBranchIdAndAlertTypeAndCurrencyAndResolvedFalse(
                inventory.getBranch().getId(), LiquidityAlert.AlertType.FORECAST_SHORTAGE, inventory.getCurrency()).isPresent();
        if (exists) {
            return false;
        }
        alertRepository.save(LiquidityAlert.builder()
                .branchId(inventory.getBranch().getId())
                .alertType(LiquidityAlert.AlertType.FORECAST_SHORTAGE)
                .currency(inventory.getCurrency())
                .currentBalance(inventory.getAvailableBalance())
                .thresholdBreached(inventory.getLowCashThreshold())
                .message("Forecast shortage risk for " + inventory.getCurrency() + " at branch "
                        + inventory.getBranch().getName() + ": 48h balance " + forecastRow.get("projectedBalance48h")
                        + ", 7d balance " + forecastRow.get("projectedBalance7d"))
                .resolved(false)
                .build());
        return true;
    }
}
