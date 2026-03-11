package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.service.trading.PriceCacheService;
import com.mycompany.transfersystem.service.trading.YahooFinanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final YahooFinanceService yahooFinanceService;
    private final PriceCacheService priceCacheService;

    public MarketDataController(YahooFinanceService yahooFinanceService, PriceCacheService priceCacheService) {
        this.yahooFinanceService = yahooFinanceService;
        this.priceCacheService = priceCacheService;
    }

    @GetMapping("/quote/{symbol}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getQuote(@PathVariable String symbol) {
        BigDecimal price = priceCacheService.get(symbol);
        if (price == null) {
            price = yahooFinanceService.getQuote(symbol).orElse(null);
            if (price != null) priceCacheService.set(symbol, price);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("symbol", symbol);
        body.put("price", price);
        return ResponseEntity.ok(body);
    }
}
