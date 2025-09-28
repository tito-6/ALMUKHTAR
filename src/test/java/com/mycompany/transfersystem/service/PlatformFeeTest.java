package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlatformFeeTest {

    private FeeCalculationService feeService;
    private Currency usd;
    private Currency eur;

    @BeforeEach
    void setUp() {
        feeService = new FeeCalculationService();

        usd = new Currency();
        usd.setCode("USD");
        usd.setExchangeRateToUsd(new BigDecimal("1.00"));

        eur = new Currency();
        eur.setCode("EUR");
        eur.setExchangeRateToUsd(new BigDecimal("1.10")); // 1 EUR = 1.10 USD
    }

    @Test
    void testPlatformBaseFeeForUsdAmounts() {
        // $1,000 -> 1 unit -> $1.50
        BigDecimal fee1k = feeService.calculatePlatformBaseFee(new BigDecimal("1000.00"), usd);
        assertEquals(new BigDecimal("1.50"), fee1k);

        // $2,000 -> 2 units -> $3.00
        BigDecimal fee2k = feeService.calculatePlatformBaseFee(new BigDecimal("2000.00"), usd);
        assertEquals(new BigDecimal("3.00"), fee2k);
    }

    @Test
    void testPlatformBaseFeeForEurAmounts() {
        // €1000 -> $1100 -> ceil(1100/1000)=2 units -> $3.00
        BigDecimal feeEur = feeService.calculatePlatformBaseFee(new BigDecimal("1000.00"), eur);
        assertEquals(new BigDecimal("3.00"), feeEur);
    }
}
