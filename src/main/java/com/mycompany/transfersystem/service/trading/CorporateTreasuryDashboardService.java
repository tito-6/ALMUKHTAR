package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.entity.ForwardContract;
import com.mycompany.transfersystem.entity.Order;
import com.mycompany.transfersystem.entity.TradingAccount;
import com.mycompany.transfersystem.entity.Watchlist;
import com.mycompany.transfersystem.entity.enums.OrderStatus;
import com.mycompany.transfersystem.repository.ForwardContractRepository;
import com.mycompany.transfersystem.repository.OrderRepository;
import com.mycompany.transfersystem.repository.TradingAccountRepository;
import com.mycompany.transfersystem.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class CorporateTreasuryDashboardService {

    private final TradingAccountRepository tradingAccountRepository;
    private final OrderRepository orderRepository;
    private final ForwardContractRepository forwardContractRepository;
    private final WatchlistRepository watchlistRepository;

    public CorporateTreasuryDashboardService(TradingAccountRepository tradingAccountRepository,
                                             OrderRepository orderRepository,
                                             ForwardContractRepository forwardContractRepository,
                                             WatchlistRepository watchlistRepository) {
        this.tradingAccountRepository = tradingAccountRepository;
        this.orderRepository = orderRepository;
        this.forwardContractRepository = forwardContractRepository;
        this.watchlistRepository = watchlistRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildDashboard(Long userId) {
        TradingAccount account = tradingAccountRepository.findByUser_Id(userId).orElse(null);

        List<ForwardContract> forwards = forwardContractRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        Map<String, BigDecimal> fxExposure = new LinkedHashMap<>();
        for (ForwardContract fc : forwards) {
            if (!"ACTIVE".equalsIgnoreCase(fc.getStatus())) {
                continue;
            }
            String pair = fc.getFromCurrency() + "/" + fc.getToCurrency();
            fxExposure.merge(pair, fc.getNotionalAmount(), BigDecimal::add);
        }

        List<Order> pending = account != null
                ? orderRepository.findByTradingAccount_IdAndStatus(account.getId(), OrderStatus.PENDING)
                : List.of();

        YearMonth ym = YearMonth.now(ZoneOffset.UTC);
        Instant monthStart = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant nextMonth = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal feesMonth = account != null
                ? nullToZero(orderRepository.sumPlatformFeesBetween(account.getId(), monthStart, nextMonth))
                : BigDecimal.ZERO;

        long tradesMonth = account != null
                ? orderRepository.countByTradingAccount_IdAndCreatedAtBetween(account.getId(), monthStart, nextMonth)
                : 0L;

        List<Watchlist> watchlists = watchlistRepository.findByUser_Id(userId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fxExposureByPair", fxExposure);
        out.put("forwardContracts", forwards.stream().limit(50).map(this::toForwardView).toList());
        out.put("pendingOrders", pending);
        out.put("feesPaidThisMonthUsd", feesMonth);
        out.put("monthlyOrderCount", tradesMonth);
        out.put("watchlists", watchlists);
        return out;
    }

    private Map<String, Object> toForwardView(ForwardContract fc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", fc.getId());
        m.put("pair", fc.getFromCurrency() + "/" + fc.getToCurrency());
        m.put("notionalAmount", fc.getNotionalAmount());
        m.put("lockedRate", fc.getLockedRate());
        m.put("status", fc.getStatus());
        m.put("executionDate", fc.getExecutionDate());
        m.put("contractFee", fc.getContractFee());
        return m;
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
