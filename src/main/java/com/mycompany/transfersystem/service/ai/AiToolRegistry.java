package com.mycompany.transfersystem.service.ai;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.CashierShift;
import com.mycompany.transfersystem.entity.Dispute;
import com.mycompany.transfersystem.entity.TopupRequest;
import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.WalletBalance;
import com.mycompany.transfersystem.entity.enums.AiAgentRole;
import com.mycompany.transfersystem.entity.enums.AiToolName;
import com.mycompany.transfersystem.entity.enums.TopupRequestStatus;
import com.mycompany.transfersystem.repository.AmlAlertRepository;
import com.mycompany.transfersystem.repository.BillPaymentRequestRepository;
import com.mycompany.transfersystem.repository.BranchCashReservationRepository;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.CashierShiftRepository;
import com.mycompany.transfersystem.repository.DisputeRepository;
import com.mycompany.transfersystem.repository.OrderRepository;
import com.mycompany.transfersystem.repository.TopupRequestRepository;
import com.mycompany.transfersystem.repository.TradingAccountRepository;
import com.mycompany.transfersystem.repository.TransactionRepository;
import com.mycompany.transfersystem.repository.WalletBalanceRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import com.mycompany.transfersystem.service.ExchangeRateService;
import com.mycompany.transfersystem.service.liquidity.LiquidityForecastService;
import com.mycompany.transfersystem.service.revenue.RevenueReportService;
import com.mycompany.transfersystem.service.wallet.WalletTopUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class AiToolRegistry {

    private static final Map<AiAgentRole, Set<AiToolName>> ALLOWED = buildMatrix();

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final BranchRepository branchRepository;
    private final WalletTopUpService walletTopUpService;
    private final LiquidityForecastService liquidityForecastService;
    private final CashierShiftRepository cashierShiftRepository;
    private final RevenueReportService revenueReportService;
    private final AmlAlertRepository amlAlertRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final OrderRepository orderRepository;
    private final ExchangeRateService exchangeRateService;
    private final TopupRequestRepository topupRequestRepository;
    private final BillPaymentRequestRepository billPaymentRequestRepository;
    private final BranchCashReservationRepository branchCashReservationRepository;
    private final DisputeRepository disputeRepository;

    private static Map<AiAgentRole, Set<AiToolName>> buildMatrix() {
        Map<AiAgentRole, Set<AiToolName>> m = new EnumMap<>(AiAgentRole.class);
        m.put(AiAgentRole.CUSTOMER_HELPER, EnumSet.of(
                AiToolName.GET_MY_TRANSACTION_STATUS,
                AiToolName.GET_MY_WALLET_SUMMARY,
                AiToolName.GET_MY_KYC_STATUS,
                AiToolName.GET_NEAREST_BRANCHES,
                AiToolName.CREATE_DISPUTE_DRAFT));
        m.put(AiAgentRole.CASHIER_COPILOT, EnumSet.of(
                AiToolName.GET_BRANCH_QUEUE_SUMMARY,
                AiToolName.GET_CASHIER_SHIFT_SUMMARY));
        m.put(AiAgentRole.BRANCH_MANAGER_COPILOT, EnumSet.of(
                AiToolName.GET_BRANCH_QUEUE_SUMMARY,
                AiToolName.GET_BRANCH_LIQUIDITY_FORECAST,
                AiToolName.GET_CASHIER_SHIFT_SUMMARY,
                AiToolName.GET_AML_CRITICAL_SUMMARY));
        m.put(AiAgentRole.PLATFORM_OWNER_COPILOT, EnumSet.allOf(AiToolName.class));
        m.put(AiAgentRole.TRADING_ASSISTANT, EnumSet.of(
                AiToolName.GET_TRADING_PORTFOLIO_SUMMARY,
                AiToolName.GET_MARKET_QUOTE,
                AiToolName.CREATE_PRICE_ALERT_DRAFT,
                AiToolName.GET_MY_WALLET_SUMMARY,
                AiToolName.GET_MY_TRANSACTION_STATUS));
        return Map.copyOf(m);
    }

    public boolean canInvoke(AiAgentRole role, AiToolName tool) {
        Set<AiToolName> set = ALLOWED.get(role);
        return set != null && set.contains(tool);
    }

    public Map<String, Object> execute(AiAgentRole role, User actor, Map<String, Object> args, AiToolName tool) {
        if (!canInvoke(role, tool)) {
            throw new AccessDeniedException("Tool not permitted for this AI role");
        }
        return switch (tool) {
            case GET_MY_TRANSACTION_STATUS -> getMyTransactionStatus(actor, args);
            case GET_MY_WALLET_SUMMARY -> getMyWalletSummary(actor, args);
            case GET_MY_KYC_STATUS -> getMyKycStatus(actor, args);
            case GET_NEAREST_BRANCHES -> getNearestBranches(args);
            case CREATE_DISPUTE_DRAFT -> createDisputeDraft(actor, args);
            case GET_BRANCH_QUEUE_SUMMARY -> getBranchQueueSummary(role, actor, args);
            case GET_BRANCH_LIQUIDITY_FORECAST -> getBranchLiquidityForecast(role, actor, args);
            case GET_CASHIER_SHIFT_SUMMARY -> getCashierShiftSummary(actor);
            case GET_PLATFORM_REVENUE_SUMMARY -> getPlatformRevenueSummary();
            case GET_AML_CRITICAL_SUMMARY -> getAmlCriticalSummary(role, actor);
            case GET_TRADING_PORTFOLIO_SUMMARY -> getTradingPortfolioSummary(actor);
            case GET_MARKET_QUOTE -> getMarketQuote(args);
            case CREATE_PRICE_ALERT_DRAFT -> createPriceAlertDraft(actor, args);
        };
    }

    private void assertNoForeignUserTarget(User caller, Map<String, Object> args) {
        if (args == null) {
            return;
        }
        for (String key : List.of("userId", "targetUserId", "forUserId")) {
            Object v = args.get(key);
            if (v == null) {
                continue;
            }
            if (!String.valueOf(v).equals(String.valueOf(caller.getId()))) {
                throw new AccessDeniedException("Cross-user lookup denied");
            }
        }
    }

    private Map<String, Object> getMyTransactionStatus(User actor, Map<String, Object> args) {
        assertNoForeignUserTarget(actor, args);
        String ref = stringArg(args, "reference");
        if (ref == null || ref.isBlank()) {
            return Map.of("error", "reference required");
        }
        Optional<Transaction> txOpt = tryFindTransaction(ref);
        if (txOpt.isEmpty()) {
            return Map.of("found", false);
        }
        Transaction t = txOpt.get();
        boolean involved = t.getSender().getId().equals(actor.getId()) || t.getReceiver().getId().equals(actor.getId());
        if (!involved) {
            return Map.of("found", false, "note", "No transaction for this reference under your account");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("found", true);
        m.put("status", t.getStatus().name());
        m.put("amount", t.getAmount());
        m.put("currency", t.getCurrencyCode());
        m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
        return m;
    }

    private Optional<Transaction> tryFindTransaction(String ref) {
        try {
            long id = Long.parseLong(ref);
            return transactionRepository.findById(id);
        } catch (NumberFormatException e) {
            return transactionRepository.findByIdempotencyKey(ref);
        }
    }

    private Map<String, Object> getMyWalletSummary(User actor, Map<String, Object> args) {
        assertNoForeignUserTarget(actor, args);
        Wallet w = walletRepository.findByUser_Id(actor.getId())
                .orElse(null);
        if (w == null) {
            return Map.of("hasWallet", false);
        }
        List<Map<String, Object>> balances = walletBalanceRepository.findByWalletIdOrderByCurrencyCode(w.getId()).stream()
                .map(this::balanceRow)
                .toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hasWallet", true);
        m.put("walletStatus", w.getStatus().name());
        m.put("balances", balances);
        return m;
    }

    private Map<String, Object> balanceRow(WalletBalance wb) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("currency", wb.getCurrencyCode());
        row.put("available", wb.getAvailableBalance());
        row.put("locked", wb.getLockedBalance());
        return row;
    }

    private Map<String, Object> getMyKycStatus(User actor, Map<String, Object> args) {
        assertNoForeignUserTarget(actor, args);
        return walletRepository.findByUser_Id(actor.getId())
                .map(w -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("kycTier", w.getKycTier().name());
                    m.put("walletStatus", w.getStatus().name());
                    m.put("dailyLimit", w.getDailyLimit());
                    m.put("monthlyLimit", w.getMonthlyLimit());
                    return m;
                })
                .orElseGet(() -> Map.of("hasWallet", false));
    }

    private Map<String, Object> getNearestBranches(Map<String, Object> args) {
        String city = stringArg(args, "city");
        List<Branch> list;
        if (city != null && !city.isBlank()) {
            list = branchRepository.findByCityIgnoreCase(city.strip());
        } else {
            list = branchRepository.findAll().stream()
                    .sorted(Comparator.comparing(Branch::getName, String.CASE_INSENSITIVE_ORDER))
                    .limit(20)
                    .toList();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Branch b : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", b.getName());
            row.put("city", b.getCity());
            row.put("country", b.getCountry());
            row.put("addressLine", b.getAddressLine());
            row.put("phone", AiDataMaskingUtil.maskPhonesAndIds(b.getPhone()));
            row.put("opensAt", b.getOpensAt() != null ? b.getOpensAt().toString() : null);
            row.put("closesAt", b.getClosesAt() != null ? b.getClosesAt().toString() : null);
            rows.add(row);
        }
        return Map.of("branches", rows, "note", "Coordinates are not returned by this tool.");
    }

    private Map<String, Object> createDisputeDraft(User actor, Map<String, Object> args) {
        assertNoForeignUserTarget(actor, args);
        String ref = stringArg(args, "transactionReference");
        String category = stringArg(args, "category");
        String description = stringArg(args, "description");
        if (ref == null || category == null) {
            return Map.of("error", "transactionReference and category required");
        }
        String token = UUID.randomUUID().toString();
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("type", "DISPUTE_DRAFT");
        draft.put("confirmationToken", token);
        draft.put("reporterUserId", actor.getId());
        draft.put("transactionReference", ref);
        draft.put("category", category);
        draft.put("description", description != null ? description : "");
        draft.put("instructions", "Confirm in app; final submission must go through POST /api/disputes with authenticated user.");
        return draft;
    }

    private Map<String, Object> getBranchQueueSummary(AiAgentRole role, User actor, Map<String, Object> args) {
        Long branchId = resolveBranchId(role, actor, args);
        if (branchId == null) {
            return Map.of("error", "branch context required");
        }
        List<TopupRequest> pending = walletTopUpService.getPendingTopUps(branchId);
        Map<Long, Long> walletCounts = pending.stream()
                .collect(Collectors.groupingBy(t -> t.getWallet().getId(), Collectors.counting()));
        List<String> duplicateRisk = walletCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> "walletId=" + e.getKey() + " pendingCount=" + e.getValue())
                .toList();
        long pendingBills = billPaymentRequestRepository.countByStatus("PENDING");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("branchId", branchId);
        m.put("pendingTopUps", pending.size());
        m.put("pendingBillPaymentsGlobal", pendingBills);
        m.put("duplicatePayoutRiskHints", duplicateRisk);
        long openDisputes = disputeRepository.findByAssignedBranchIdAndStatusIn(branchId,
                List.of(Dispute.DisputeStatus.OPEN, Dispute.DisputeStatus.UNDER_REVIEW)).size();
        m.put("openDisputesAtBranch", openDisputes);
        return m;
    }

    private Map<String, Object> getBranchLiquidityForecast(AiAgentRole role, User actor, Map<String, Object> args) {
        Long branchId = resolveBranchId(role, actor, args);
        if (branchId == null) {
            return Map.of("error", "branch context required");
        }
        Map<String, Object> forecast = liquidityForecastService.getForecast(branchId);
        BigDecimal reserved = branchCashReservationRepository.sumReservedAmountByBranch(
                branchId, com.mycompany.transfersystem.entity.BranchCashReservation.ReservationStatus.RESERVED);
        forecast.put("reservedCashTotal", reserved != null ? reserved : BigDecimal.ZERO);
        forecast.put("cashReservationPressureNote", "Higher reserved totals reduce free cash for walk-in payouts.");
        return forecast;
    }

    private Map<String, Object> getCashierShiftSummary(User actor) {
        Optional<CashierShift> open = cashierShiftRepository.findByCashierIdAndStatus(actor.getId(), CashierShift.ShiftStatus.OPEN);
        if (open.isEmpty()) {
            return Map.of("openShift", false);
        }
        CashierShift s = open.get();
        Instant since = Instant.now().minus(1, ChronoUnit.DAYS);
        long completed = 0;
        if (s.getBranch() != null) {
            completed = topupRequestRepository.countByBranch_IdAndCashier_IdAndStatusAndCompletedAtAfter(
                    s.getBranch().getId(), actor.getId(), TopupRequestStatus.COMPLETED, since);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("openShift", true);
        m.put("shiftId", s.getId());
        m.put("branchId", s.getBranch() != null ? s.getBranch().getId() : null);
        m.put("openedAt", s.getOpenedAt() != null ? s.getOpenedAt().toString() : null);
        m.put("completedTopUpsLast24h", completed);
        return m;
    }

    private Map<String, Object> getPlatformRevenueSummary() {
        Map<String, Object> snap = revenueReportService.getDashboardSnapshot();
        snap.put("branchCount", branchRepository.count());
        snap.put("note", "WhatsApp delivery failures are not included in this summary.");
        return snap;
    }

    private Map<String, Object> getAmlCriticalSummary(AiAgentRole role, User actor) {
        List<com.mycompany.transfersystem.entity.AmlAlert> critical = amlAlertRepository
                .findTop30ByStatusAndSeverityOrderByCreatedAtDesc("OPEN", "CRITICAL");
        if (role == AiAgentRole.BRANCH_MANAGER_COPILOT) {
            Long bid = actor.getBranch() != null ? actor.getBranch().getId() : null;
            if (bid == null) {
                return Map.of("items", List.of(), "note", "No branch assigned to user");
            }
            List<Map<String, Object>> scoped = critical.stream()
                    .filter(a -> a.getUser().getBranch() != null && bid.equals(a.getUser().getBranch().getId()))
                    .map(this::amlRow)
                    .toList();
            return Map.of("severity", "CRITICAL", "items", scoped);
        }
        List<Map<String, Object>> rows = critical.stream().map(this::amlRow).toList();
        return Map.of("severity", "CRITICAL", "items", rows);
    }

    private Map<String, Object> amlRow(com.mycompany.transfersystem.entity.AmlAlert a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("userId", a.getUser().getId());
        m.put("status", a.getStatus());
        m.put("severity", a.getSeverity());
        m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> getTradingPortfolioSummary(User actor) {
        return tradingAccountRepository.findByUser_Id(actor.getId())
                .map(acc -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("accountId", acc.getId());
                    m.put("status", acc.getStatus());
                    m.put("buyingPowerUsd", acc.getBuyingPowerUsd());
                    m.put("totalPortfolioValue", acc.getTotalPortfolioValue());
                    m.put("openOrders", orderRepository.findByTradingAccount_IdAndStatus(acc.getId(),
                            com.mycompany.transfersystem.entity.enums.OrderStatus.PENDING).size());
                    m.put("recentClosedOrders",
                            orderRepository.findByTradingAccount_IdOrderByCreatedAtDesc(acc.getId(), PageRequest.of(0, 5))
                                    .getContent().stream()
                                    .map(o -> Map.<String, Object>of(
                                            "id", o.getId(),
                                            "status", o.getStatus().name(),
                                            "side", o.getSide().name()))
                                    .toList());
                    return m;
                })
                .orElseGet(() -> Map.of("hasTradingAccount", false));
    }

    private Map<String, Object> getMarketQuote(Map<String, Object> args) {
        String from = Optional.ofNullable(stringArg(args, "fromCurrency")).orElse("USD");
        String to = Optional.ofNullable(stringArg(args, "toCurrency")).orElse("EUR");
        BigDecimal rate = exchangeRateService.getRate(from, to);
        return Map.of("fromCurrency", from, "toCurrency", to, "rate", rate,
                "disclaimer", "Indicative rate only; execution prices may differ.");
    }

    private Map<String, Object> createPriceAlertDraft(User actor, Map<String, Object> args) {
        assertNoForeignUserTarget(actor, args);
        String symbol = stringArg(args, "symbol");
        String target = stringArg(args, "targetPrice");
        if (symbol == null || target == null) {
            return Map.of("error", "symbol and targetPrice required");
        }
        String token = UUID.randomUUID().toString();
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("type", "PRICE_ALERT_DRAFT");
        draft.put("confirmationToken", token);
        draft.put("symbol", symbol);
        draft.put("targetPrice", target);
        draft.put("instructions", "Confirm in app; creating the alert must use authenticated trading endpoints after review.");
        return draft;
    }

    private Long resolveBranchId(AiAgentRole role, User actor, Map<String, Object> args) {
        Long fromArgs = longArg(args, "branchId");
        if (role == AiAgentRole.PLATFORM_OWNER_COPILOT) {
            return fromArgs != null ? fromArgs : (actor.getBranch() != null ? actor.getBranch().getId() : null);
        }
        if (role == AiAgentRole.CASHIER_COPILOT || role == AiAgentRole.BRANCH_MANAGER_COPILOT) {
            if (actor.getBranch() == null) {
                return null;
            }
            if (fromArgs != null && !fromArgs.equals(actor.getBranch().getId())) {
                throw new AccessDeniedException("branch mismatch");
            }
            return actor.getBranch().getId();
        }
        return actor.getBranch() != null ? actor.getBranch().getId() : null;
    }

    private String stringArg(Map<String, Object> args, String key) {
        if (args == null || args.get(key) == null) {
            return null;
        }
        return String.valueOf(args.get(key));
    }

    private Long longArg(Map<String, Object> args, String key) {
        Object v = args != null ? args.get(key) : null;
        if (v == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
