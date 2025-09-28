package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.Currency;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CurrencyConversionService {

    @Autowired
    private CurrencyRepository currencyRepository;

    @org.springframework.beans.factory.annotation.Value("${app.exchange.margin.rate:0.0015}")
    private BigDecimal marginRate;

    /**
     * Convert amount from one currency to another
     * @param amount Amount to convert
     * @param fromCurrencyCode Source currency code
     * @param toCurrencyCode Target currency code
     * @return Converted amount
     */
    public BigDecimal convertCurrency(BigDecimal amount, String fromCurrencyCode, String toCurrencyCode) {
        if (fromCurrencyCode.equals(toCurrencyCode)) {
            return amount;
        }

        Currency fromCurrency = currencyRepository.findByCodeAndIsActiveTrue(fromCurrencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + fromCurrencyCode));

        Currency toCurrency = currencyRepository.findByCodeAndIsActiveTrue(toCurrencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + toCurrencyCode));

        // Convert to USD first, then to target currency, applying margin
        BigDecimal amountInUsd = convertToUsdWithMargin(amount, fromCurrency);
        return convertFromUsdWithMargin(amountInUsd, toCurrency);
    }

    /**
     * Convert amount to USD
     * @param amount Amount in source currency
     * @param fromCurrency Source currency entity
     * @return Amount in USD
     */
    // Applies forexBuying minus margin for inbound conversion (A -> USD)
    public BigDecimal convertToUsdWithMargin(BigDecimal amount, Currency fromCurrency) {
        if ("USD".equals(fromCurrency.getCode())) {
            return amount;
        }
        BigDecimal buyingRate = fromCurrency.getForexBuyingToUsd() != null ? fromCurrency.getForexBuyingToUsd() : fromCurrency.getExchangeRateToUsd();
        BigDecimal adjustedRate = buyingRate.multiply(BigDecimal.ONE.subtract(marginRate));
        return amount.multiply(adjustedRate).setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Convert amount from USD to target currency
     * @param amountInUsd Amount in USD
     * @param toCurrency Target currency entity
     * @return Amount in target currency
     */
    // Applies forexSelling plus margin for outbound conversion (USD -> B)
    public BigDecimal convertFromUsdWithMargin(BigDecimal amountInUsd, Currency toCurrency) {
        if ("USD".equals(toCurrency.getCode())) {
            return amountInUsd;
        }
        BigDecimal sellingRate = toCurrency.getForexSellingToUsd() != null ? toCurrency.getForexSellingToUsd() : toCurrency.getExchangeRateToUsd();
        BigDecimal adjustedRate = sellingRate.multiply(BigDecimal.ONE.add(marginRate));
        return amountInUsd.divide(adjustedRate, 8, RoundingMode.HALF_UP);
    }

    /**
     * Get exchange rate between two currencies
     * @param fromCurrencyCode Source currency
     * @param toCurrencyCode Target currency
     * @return Exchange rate
     */
    // Returns the official rate (mid) for audit
    public BigDecimal getOfficialRate(String fromCurrencyCode, String toCurrencyCode) {
        if (fromCurrencyCode.equals(toCurrencyCode)) {
            return BigDecimal.ONE;
        }
        Currency fromCurrency = currencyRepository.findByCodeAndIsActiveTrue(fromCurrencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + fromCurrencyCode));
        Currency toCurrency = currencyRepository.findByCodeAndIsActiveTrue(toCurrencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + toCurrencyCode));
        return fromCurrency.getExchangeRateToUsd().divide(toCurrency.getExchangeRateToUsd(), 8, RoundingMode.HALF_UP);
    }

    // Returns the applied rate (with margin)
    public BigDecimal getAppliedRate(String fromCurrencyCode, String toCurrencyCode) {
        if (fromCurrencyCode.equals(toCurrencyCode)) {
            return BigDecimal.ONE;
        }
        Currency fromCurrency = currencyRepository.findByCodeAndIsActiveTrue(fromCurrencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + fromCurrencyCode));
        Currency toCurrency = currencyRepository.findByCodeAndIsActiveTrue(toCurrencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + toCurrencyCode));
        // If converting from A -> USD, use buying minus margin
        if ("USD".equals(toCurrencyCode)) {
            BigDecimal buyingRate = fromCurrency.getForexBuyingToUsd() != null ? fromCurrency.getForexBuyingToUsd() : fromCurrency.getExchangeRateToUsd();
            return buyingRate.multiply(BigDecimal.ONE.subtract(marginRate));
        }
        // If converting from USD -> B, use selling plus margin
        if ("USD".equals(fromCurrencyCode)) {
            BigDecimal sellingRate = toCurrency.getForexSellingToUsd() != null ? toCurrency.getForexSellingToUsd() : toCurrency.getExchangeRateToUsd();
            return BigDecimal.ONE.divide(sellingRate.multiply(BigDecimal.ONE.add(marginRate)), 8, RoundingMode.HALF_UP);
        }
        // For A -> B, convert to USD then to B
        BigDecimal buyingRate = fromCurrency.getForexBuyingToUsd() != null ? fromCurrency.getForexBuyingToUsd() : fromCurrency.getExchangeRateToUsd();
        BigDecimal sellingRate = toCurrency.getForexSellingToUsd() != null ? toCurrency.getForexSellingToUsd() : toCurrency.getExchangeRateToUsd();
        BigDecimal adjustedBuying = buyingRate.multiply(BigDecimal.ONE.subtract(marginRate));
        BigDecimal adjustedSelling = sellingRate.multiply(BigDecimal.ONE.add(marginRate));
        return adjustedBuying.divide(adjustedSelling, 8, RoundingMode.HALF_UP);
    }

    /**
     * Get exchange rate between two currencies (alias for getAppliedRate)
     * @param fromCurrencyCode Source currency
     * @param toCurrencyCode Target currency
     * @return Exchange rate
     */
    public BigDecimal getExchangeRate(String fromCurrencyCode, String toCurrencyCode) {
        return getAppliedRate(fromCurrencyCode, toCurrencyCode);
    }

    /**
     * Format amount with currency symbol
     * @param amount Amount to format
     * @param currencyCode Currency code
     * @return Formatted string
     */
    public String formatAmount(BigDecimal amount, String currencyCode) {
        Currency currency = currencyRepository.findByCodeAndIsActiveTrue(currencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + currencyCode));

        String symbol = currency.getSymbol() != null ? currency.getSymbol() : currency.getCode();
        return symbol + " " + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
}