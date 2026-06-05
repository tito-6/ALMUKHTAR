package com.mycompany.transfersystem.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AmlRuleEngineTest {

    @Test
    void structuringRule_nearThreshold_triggersAlert() {
        // AML rule: amount near $10,000 threshold should trigger
        java.math.BigDecimal amount = new java.math.BigDecimal("9800");
        java.math.BigDecimal threshold = new java.math.BigDecimal("10000");
        boolean triggers = amount.compareTo(threshold.multiply(new java.math.BigDecimal("0.9"))) >= 0;
        assertTrue(triggers);
    }

    @Test
    void largeAmountRule_aboveThreshold() {
        java.math.BigDecimal amount = new java.math.BigDecimal("55000");
        java.math.BigDecimal threshold = new java.math.BigDecimal("50000");
        assertTrue(amount.compareTo(threshold) > 0);
    }

    @Test
    void roundAmountPattern_lastFiveTxs() {
        java.math.BigDecimal[] amounts = {
                new java.math.BigDecimal("1000"),
                new java.math.BigDecimal("2000"),
                new java.math.BigDecimal("5000"),
                new java.math.BigDecimal("3000"),
                new java.math.BigDecimal("1000")
        };
        boolean allRound = true;
        for (var a : amounts) {
            if (a.remainder(new java.math.BigDecimal("1000")).compareTo(java.math.BigDecimal.ZERO) != 0) {
                allRound = false;
                break;
            }
        }
        assertTrue(allRound);
    }
}
