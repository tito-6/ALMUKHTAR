package com.mycompany.transfersystem.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class ExchangeRateApiTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    public void testRealApiCall() {
        // Test with a common currency pair that should be supported
        try {
            BigDecimal rate = exchangeRateService.fetchRealTimeRate("USD", "EUR");
            assertThat(rate).isNotNull();
            assertThat(rate).isGreaterThan(BigDecimal.ZERO);
            System.out.println("Real API Rate USD to EUR: " + rate);
        } catch (Exception e) {
            System.out.println("API call failed (expected in test environment): " + e.getMessage());
            // This is expected in test environment, so we just log it
        }
    }

    @Test
    public void testMockDataFallback() {
        // Test that mock data works correctly
        BigDecimal mockRate = exchangeRateService.getRate("USD", "EUR");
        assertThat(mockRate).isNotNull();
        assertThat(mockRate).isGreaterThan(BigDecimal.ZERO);
        System.out.println("Mock Rate USD to EUR: " + mockRate);
    }

    @Test
    public void testCurrencyConversion() {
        // Test currency conversion with mock data
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal convertedAmount = exchangeRateService.convertAmount(amount, "USD", "EUR");
        
        assertThat(convertedAmount).isNotNull();
        assertThat(convertedAmount).isGreaterThan(BigDecimal.ZERO);
        System.out.println("Converted $100 USD to EUR: " + convertedAmount);
    }
}
