package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.service.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange-rates")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Autowired
    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * Get exchange rate between two currencies
     * GET /api/exchange-rates/{fromCurrency}/{toCurrency}
     */
    @GetMapping("/{fromCurrency}/{toCurrency}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('CASHIER') or hasRole('AUDITOR')")
    public ResponseEntity<Map<String, Object>> getExchangeRate(
            @PathVariable String fromCurrency,
            @PathVariable String toCurrency) {
        
        try {
            BigDecimal rate = exchangeRateService.getRate(fromCurrency, toCurrency);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fromCurrency", fromCurrency.toUpperCase());
            response.put("toCurrency", toCurrency.toUpperCase());
            response.put("rate", rate);
            response.put("source", exchangeRateService.isApiAvailable() ? "ExchangeRate-API.com" : "Mock Data");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get exchange rate: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Convert amount between currencies
     * POST /api/exchange-rates/convert
     */
    @PostMapping("/convert")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('CASHIER') or hasRole('AUDITOR')")
    public ResponseEntity<Map<String, Object>> convertAmount(
            @RequestBody Map<String, Object> request) {
        
        try {
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String fromCurrency = request.get("fromCurrency").toString();
            String toCurrency = request.get("toCurrency").toString();
            
            BigDecimal convertedAmount = exchangeRateService.convertAmount(amount, fromCurrency, toCurrency);
            BigDecimal rate = exchangeRateService.getRate(fromCurrency, toCurrency);
            
            Map<String, Object> response = new HashMap<>();
            response.put("originalAmount", amount);
            response.put("fromCurrency", fromCurrency.toUpperCase());
            response.put("toCurrency", toCurrency.toUpperCase());
            response.put("rate", rate);
            response.put("convertedAmount", convertedAmount);
            response.put("source", exchangeRateService.isApiAvailable() ? "ExchangeRate-API.com" : "Mock Data");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to convert amount: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get API quota information
     * GET /api/exchange-rates/quota
     */
    @GetMapping("/quota")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<String> getApiQuota() {
        String quotaInfo = exchangeRateService.getApiQuotaInfo();
        return ResponseEntity.ok(quotaInfo);
    }

    /**
     * Get supported currencies
     * GET /api/exchange-rates/currencies
     */
    @GetMapping("/currencies")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('CASHIER') or hasRole('AUDITOR')")
    public ResponseEntity<String> getSupportedCurrencies() {
        String currencies = exchangeRateService.getSupportedCurrencies();
        return ResponseEntity.ok(currencies);
    }

    /**
     * Check API health
     * GET /api/exchange-rates/health
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Map<String, Object>> checkApiHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("apiAvailable", exchangeRateService.isApiAvailable());
        response.put("apiSource", "ExchangeRate-API.com");
        response.put("apiKey", "15d2ebbcc9a0bb27541b71fd");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all conversion rates for a base currency
     * GET /api/exchange-rates/all/{baseCurrency}
     */
    @GetMapping("/all/{baseCurrency}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<String> getAllRates(@PathVariable String baseCurrency) {
        String allRates = exchangeRateService.fetchAllRates(baseCurrency);
        return ResponseEntity.ok(allRates);
    }

    /**
     * Get API metadata (last update time, next update time, etc.)
     * GET /api/exchange-rates/metadata/{baseCurrency}
     */
    @GetMapping("/metadata/{baseCurrency}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<String> getApiMetadata(@PathVariable String baseCurrency) {
        String metadata = exchangeRateService.getApiMetadata(baseCurrency);
        return ResponseEntity.ok(metadata);
    }
}
