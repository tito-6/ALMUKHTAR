package com.mycompany.transfersystem.aop;

import com.mycompany.transfersystem.annotation.PlatformRevenue;
import com.mycompany.transfersystem.service.revenue.PlatformRevenueService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Intercepts @PlatformRevenue methods and collects revenue after successful execution.
 * Fee amount is extracted from return value if it has getPlatformFee() or getFee().
 */
@Aspect
@Component
public class PlatformRevenueAspect {

    private static final Logger log = LoggerFactory.getLogger(PlatformRevenueAspect.class);

    private final PlatformRevenueService platformRevenueService;

    public PlatformRevenueAspect(PlatformRevenueService platformRevenueService) {
        this.platformRevenueService = platformRevenueService;
    }

    @Around("@annotation(platformRevenue)")
    public Object interceptRevenue(ProceedingJoinPoint jp, PlatformRevenue platformRevenue) throws Throwable {
        Object result = jp.proceed();
        try {
            BigDecimal feeAmount = extractFee(result);
            if (feeAmount != null && feeAmount.compareTo(BigDecimal.ZERO) > 0) {
                String currency = extractCurrency(result);
                Long entityId = extractEntityId(result);
                platformRevenueService.collect(platformRevenue.event(), feeAmount, currency, entityId, null);
            }
        } catch (Exception e) {
            log.error("Platform revenue collection failed (non-fatal): {}", e.getMessage());
        }
        return result;
    }

    private BigDecimal extractFee(Object result) {
        if (result == null) return null;
        try {
            if (result instanceof java.math.BigDecimal) return (BigDecimal) result;
            var getter = result.getClass().getMethod("getPlatformFee");
            Object v = getter.invoke(result);
            return v instanceof BigDecimal ? (BigDecimal) v : null;
        } catch (NoSuchMethodException e) {
            try {
                var getter = result.getClass().getMethod("getFee");
                Object v = getter.invoke(result);
                return v instanceof BigDecimal ? (BigDecimal) v : null;
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return null;
    }

    private String extractCurrency(Object result) {
        if (result == null) return "USD";
        try {
            var getter = result.getClass().getMethod("getCurrency");
            Object v = getter.invoke(result);
            return v != null ? v.toString() : "USD";
        } catch (Exception e) {
            return "USD";
        }
    }

    private Long extractEntityId(Object result) {
        if (result == null) return null;
        try {
            var getter = result.getClass().getMethod("getId");
            Object v = getter.invoke(result);
            return v instanceof Long ? (Long) v : null;
        } catch (Exception e) {
            return null;
        }
    }
}
