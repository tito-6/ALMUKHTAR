package com.mycompany.transfersystem.service.trading;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory price cache with TTL (e.g. 2 seconds). Can be replaced with Redis when available.
 */
@Service
public class PriceCacheService {

    private static final long TTL_MS = 2_000;

    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    public BigDecimal get(String symbol) {
        CachedQuote c = cache.get(symbol);
        if (c == null || System.currentTimeMillis() > c.expiresAt) return null;
        return c.price;
    }

    public void set(String symbol, BigDecimal price) {
        cache.put(symbol, new CachedQuote(price, System.currentTimeMillis() + TTL_MS));
    }

    private record CachedQuote(BigDecimal price, long expiresAt) {}
}
