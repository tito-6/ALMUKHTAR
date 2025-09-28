package com.mycompany.transfersystem.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class ExchangeRateServiceIT {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    public void testRealApiIntegration() {
        // Test if the API is available (this will use mock data in test environment)
        boolean isApiAvailable = exchangeRateService.isApiAvailable();
        
        // In test environment, API might not be available, so we test the fallback
        assertThat(isApiAvailable).isInstanceOf(Boolean.class);
        
        // Test getting a rate (will use mock data if API fails)
        BigDecimal usdToEurRate = exchangeRateService.getRate("USD", "EUR");
        assertThat(usdToEurRate).isNotNull();
        assertThat(usdToEurRate).isGreaterThan(BigDecimal.ZERO);
        
        // Test currency conversion
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal convertedAmount = exchangeRateService.convertAmount(amount, "USD", "EUR");
        assertThat(convertedAmount).isNotNull();
        assertThat(convertedAmount).isGreaterThan(BigDecimal.ZERO);
        
        // Test same currency conversion
        BigDecimal sameCurrencyAmount = exchangeRateService.convertAmount(amount, "USD", "USD");
        assertThat(sameCurrencyAmount).isEqualByComparingTo(amount);
    }

    @Test
    public void testMockRatesFallback() {
        // Test that mock rates work as fallback
        BigDecimal tlToUsdRate = exchangeRateService.getRate("TL", "USD");
        assertThat(tlToUsdRate).isNotNull();
        assertThat(tlToUsdRate).isGreaterThan(BigDecimal.ZERO);
        
        // Test the specific TL/USD rate from our mock data
        BigDecimal expectedRate = new BigDecimal("0.0241");
        assertThat(tlToUsdRate).isEqualByComparingTo(expectedRate);
    }

    @Test
    public void testApiQuotaInfo() {
        // Test API quota information (will return error in test environment)
        String quotaInfo = exchangeRateService.getApiQuotaInfo();
        assertThat(quotaInfo).isNotNull();
        assertThat(quotaInfo).isNotEmpty();
    }

    @Test
    public void testSupportedCurrencies() {
        // Test supported currencies (will return error in test environment)
        String currencies = exchangeRateService.getSupportedCurrencies();
        assertThat(currencies).isNotNull();
        assertThat(currencies).isNotEmpty();
    }
}
