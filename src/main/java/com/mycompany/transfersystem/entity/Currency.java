package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "currencies", indexes = {
        @Index(name = "idx_currency_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10, unique = true)
    private String code; // e.g., USD, EUR

    @Column(length = 100)
    private String name;

    // Exchange rate to USD (1.0 for USD)
    @Column(name = "exchange_rate_to_usd", precision = 19, scale = 8)
    private BigDecimal exchangeRateToUsd;

    // Forex spread: branch buys currency from client using this rate (to USD)
    @Column(name = "forex_buying_to_usd", precision = 19, scale = 8)
    private BigDecimal forexBuyingToUsd;

    // Forex spread: branch sells currency to client using this rate (to USD)
    @Column(name = "forex_selling_to_usd", precision = 19, scale = 8)
    private BigDecimal forexSellingToUsd;

    @Column(length = 10)
    private String symbol;

    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_manual")
    private Boolean isManual = false; // Flag to indicate if rate is managed manually
    
    @Column(name = "source_api", length = 100)
    private String sourceApi; // To track which API updated the rate

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    public Currency(String code, String name, BigDecimal exchangeRateToUsd, String symbol) {
        this.code = code;
        this.name = name;
        this.exchangeRateToUsd = exchangeRateToUsd;
        this.symbol = symbol;
        this.isActive = true;
        this.isManual = false;
        this.sourceApi = "DEFAULT";
        // Default forex buying/selling fallback to official rate if not provided
        this.forexBuyingToUsd = exchangeRateToUsd;
        this.forexSellingToUsd = exchangeRateToUsd;
    }
    
    public Currency(String code, String name, BigDecimal exchangeRateToUsd, String symbol, Boolean isManual, String sourceApi) {
        this.code = code;
        this.name = name;
        this.exchangeRateToUsd = exchangeRateToUsd;
        this.symbol = symbol;
        this.isActive = true;
        this.isManual = isManual;
        this.sourceApi = sourceApi;
        this.forexBuyingToUsd = exchangeRateToUsd;
        this.forexSellingToUsd = exchangeRateToUsd;
    }

}