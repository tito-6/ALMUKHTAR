package com.mycompany.transfersystem.service.trading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Fetches quote data from Yahoo Finance v8 chart API.
 */
@Service
public class YahooFinanceService {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceService.class);
    private static final String CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1m&range=1d&includePrePost=false";

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<BigDecimal> getQuote(String symbol) {
        return getQuoteDetails(symbol).map(MarketQuote::price);
    }

    public Optional<MarketQuote> getQuoteDetails(String symbol) {
        try {
            String url = String.format(CHART_URL, symbol);
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return Optional.empty();
            return parseFromChartJson(json);
        } catch (Exception e) {
            log.warn("Yahoo Finance quote failed for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<MarketQuote> parseFromChartJson(String json) {
        try {
            Optional<BigDecimal> price = extractNumber(json, "\"regularMarketPrice\":");
            if (price.isEmpty()) return Optional.empty();
            Optional<Long> vol = extractLong(json, "\"regularMarketVolume\":");
            return Optional.of(new MarketQuote(price.get(), vol.orElse(null)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> extractNumber(String json, String key) {
        int metaIdx = json.indexOf(key);
        if (metaIdx < 0) return Optional.empty();
        int start = metaIdx + key.length();
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);
        String value = json.substring(start, end).trim();
        if ("null".equalsIgnoreCase(value)) return Optional.empty();
        return Optional.of(new BigDecimal(value));
    }

    private Optional<Long> extractLong(String json, String key) {
        int metaIdx = json.indexOf(key);
        if (metaIdx < 0) return Optional.empty();
        int start = metaIdx + key.length();
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);
        String value = json.substring(start, end).trim();
        if ("null".equalsIgnoreCase(value)) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(value.split("\\.")[0]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public record MarketQuote(BigDecimal price, Long volume) {}
}
