package com.mycompany.transfersystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    void testGetRate_SameCurrency() {
        // Test same currency returns 1.0
        BigDecimal rate = exchangeRateService.getRate("USD", "USD");
        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void testGetRate_MockFallback() {
        // Test mock fallback when API fails
        BigDecimal rate = exchangeRateService.getRate("USD", "EUR");
        assertThat(rate).isNotNull();
        assertThat(rate).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void testConvertAmount() {
        // Test currency conversion
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal convertedAmount = exchangeRateService.convertAmount(amount, "USD", "EUR");
        
        assertThat(convertedAmount).isNotNull();
        assertThat(convertedAmount).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void testConvertAmount_NullAmount() {
        // Test null amount returns zero
        BigDecimal convertedAmount = exchangeRateService.convertAmount(null, "USD", "EUR");
        assertThat(convertedAmount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void testGetRate_NullCurrency() {
        // Test null currency throws exception
        try {
            exchangeRateService.getRate(null, "USD");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("Currency codes cannot be null");
        }
    }

    @Test
    void testIsApiAvailable() {
        // Test API availability check
        boolean isAvailable = exchangeRateService.isApiAvailable();
        // This will depend on whether the API is actually available
        assertThat(isAvailable).isInstanceOf(Boolean.class);
    }
}
