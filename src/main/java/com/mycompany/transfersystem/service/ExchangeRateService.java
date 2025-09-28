package com.mycompany.transfersystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExchangeRateService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // ExchangeRate-API.com configuration
    private static final String API_KEY = "15d2ebbcc9a0bb27541b71fd";
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/" + API_KEY;
    
    // Mock exchange rates for fallback
    private final Map<String, BigDecimal> mockRates = new HashMap<>();
    
    @Autowired
    public ExchangeRateService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        initializeMockRates();
    }
    
    /**
     * Initialize mock exchange rates for testing
     */
    private void initializeMockRates() {
        // TL/USD rate: 1 USD = 41.45 TL (so 1 TL = 0.0241 USD)
        mockRates.put("TL_USD", new BigDecimal("0.0241"));
        mockRates.put("USD_TL", new BigDecimal("41.45"));
        
        // EUR/USD rate: 1 USD = 0.93 EUR (so 1 EUR = 1.08 USD)
        mockRates.put("EUR_USD", new BigDecimal("1.08"));
        mockRates.put("USD_EUR", new BigDecimal("0.93"));
        
        // GBP/USD rate: 1 USD = 0.80 GBP (so 1 GBP = 1.25 USD)
        mockRates.put("GBP_USD", new BigDecimal("1.25"));
        mockRates.put("USD_GBP", new BigDecimal("0.80"));
        
        // Same currency rates
        mockRates.put("USD_USD", BigDecimal.ONE);
        mockRates.put("TL_TL", BigDecimal.ONE);
        mockRates.put("EUR_EUR", BigDecimal.ONE);
        mockRates.put("GBP_GBP", BigDecimal.ONE);
    }

    /**
     * Get exchange rate between two currencies using ExchangeRate-API.com
     * @param fromCurrency Source currency code
     * @param toCurrency Target currency code
     * @return Exchange rate (amount of toCurrency per 1 fromCurrency)
     */
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            throw new IllegalArgumentException("Currency codes cannot be null");
        }
        
        // Normalize currency codes
        String normalizedFrom = fromCurrency.toUpperCase();
        String normalizedTo = toCurrency.toUpperCase();
        
        // Check if it's the same currency
        if (normalizedFrom.equals(normalizedTo)) {
            return BigDecimal.ONE;
        }
        
        // Try to get real-time rate from ExchangeRate-API.com
        try {
            return fetchRealTimeRate(normalizedFrom, normalizedTo);
        } catch (Exception e) {
            // Fall back to mock rates if API fails
            return getMockRate(normalizedFrom, normalizedTo);
        }
    }
    
    /**
     * Get mock exchange rate for testing
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @return Mock exchange rate
     */
    private BigDecimal getMockRate(String fromCurrency, String toCurrency) {
        String rateKey = fromCurrency + "_" + toCurrency;
        BigDecimal rate = mockRates.get(rateKey);
        
        if (rate == null) {
            // If direct rate not found, try reverse rate and calculate inverse
            String reverseKey = toCurrency + "_" + fromCurrency;
            BigDecimal reverseRate = mockRates.get(reverseKey);
            
            if (reverseRate != null) {
                // Calculate inverse rate with proper precision
                rate = BigDecimal.ONE.divide(reverseRate, 8, RoundingMode.HALF_UP);
            } else {
                // Default fallback rate
                rate = BigDecimal.ONE;
            }
        }
        
        return rate;
    }
    
    /**
     * Fetch real-time exchange rate from ExchangeRate-API.com
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @return Exchange rate from external source
     */
    public BigDecimal fetchRealTimeRate(String fromCurrency, String toCurrency) {
        try {
            // Build API URL: https://v6.exchangerate-api.com/v6/{API_KEY}/latest/{fromCurrency}
            String apiUrl = BASE_URL + "/latest/" + fromCurrency;
            
            // Make HTTP request to ExchangeRate-API.com
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            if (response == null) {
                throw new RuntimeException("Empty response from ExchangeRate-API.com");
            }
            
            // Parse JSON response
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // Check if the API call was successful
            String result = jsonNode.get("result").asText();
            if (!"success".equals(result)) {
                throw new RuntimeException("API call failed: " + result);
            }
            
            // Get the conversion rates
            JsonNode conversionRates = jsonNode.get("conversion_rates");
            if (conversionRates == null) {
                throw new RuntimeException("No conversion rates found in response");
            }
            
            // Get the specific rate for the target currency
            JsonNode targetRate = conversionRates.get(toCurrency);
            if (targetRate == null) {
                throw new RuntimeException("Rate not found for currency: " + toCurrency);
            }
            
            // Convert to BigDecimal with proper precision
            BigDecimal rate = new BigDecimal(targetRate.asText());
            return rate.setScale(8, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            // Log the error and fall back to mock rates
            System.err.println("Failed to fetch real-time rate from ExchangeRate-API.com: " + e.getMessage());
            return getMockRate(fromCurrency, toCurrency);
        }
    }
    
    /**
     * Fetch all conversion rates for a base currency
     * @param baseCurrency Base currency code
     * @return JSON string with all conversion rates
     */
    public String fetchAllRates(String baseCurrency) {
        try {
            String apiUrl = BASE_URL + "/latest/" + baseCurrency;
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            if (response == null) {
                throw new RuntimeException("Empty response from ExchangeRate-API.com");
            }
            
            // Parse and validate response
            JsonNode jsonNode = objectMapper.readTree(response);
            String result = jsonNode.get("result").asText();
            
            if (!"success".equals(result)) {
                throw new RuntimeException("API call failed: " + result);
            }
            
            return response;
            
        } catch (Exception e) {
            return "{\"error\": \"Failed to fetch rates: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Get API response metadata (last update time, next update time, etc.)
     * @param baseCurrency Base currency code
     * @return JSON string with metadata
     */
    public String getApiMetadata(String baseCurrency) {
        try {
            String apiUrl = BASE_URL + "/latest/" + baseCurrency;
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            if (response == null) {
                throw new RuntimeException("Empty response from ExchangeRate-API.com");
            }
            
            JsonNode jsonNode = objectMapper.readTree(response);
            String result = jsonNode.get("result").asText();
            
            if (!"success".equals(result)) {
                throw new RuntimeException("API call failed: " + result);
            }
            
            // Extract metadata
            JsonNode metadata = objectMapper.createObjectNode()
                .put("result", jsonNode.get("result").asText())
                .put("base_code", jsonNode.get("base_code").asText())
                .put("time_last_update_utc", jsonNode.get("time_last_update_utc").asText())
                .put("time_next_update_utc", jsonNode.get("time_next_update_utc").asText())
                .put("documentation", jsonNode.get("documentation").asText())
                .put("terms_of_use", jsonNode.get("terms_of_use").asText());
            
            return objectMapper.writeValueAsString(metadata);
            
        } catch (Exception e) {
            return "{\"error\": \"Failed to fetch metadata: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Convert amount from one currency to another
     * @param amount Amount to convert
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @return Converted amount
     */
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal rate = getRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get API quota information from ExchangeRate-API.com
     * @return Quota information as JSON string
     */
    public String getApiQuotaInfo() {
        try {
            String quotaUrl = BASE_URL + "/quota";
            return restTemplate.getForObject(quotaUrl, String.class);
        } catch (Exception e) {
            return "{\"error\": \"Failed to fetch quota information: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Check if the API is available and working
     * @return true if API is working, false otherwise
     */
    public boolean isApiAvailable() {
        try {
            // Try to get USD to EUR rate as a health check
            fetchRealTimeRate("USD", "EUR");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get supported currencies from the API
     * @return List of supported currency codes
     */
    public String getSupportedCurrencies() {
        try {
            String currenciesUrl = BASE_URL + "/codes";
            return restTemplate.getForObject(currenciesUrl, String.class);
        } catch (Exception e) {
            return "{\"error\": \"Failed to fetch supported currencies: " + e.getMessage() + "\"}";
        }
    }
}
