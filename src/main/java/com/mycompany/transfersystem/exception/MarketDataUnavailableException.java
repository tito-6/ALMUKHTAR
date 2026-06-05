package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class MarketDataUnavailableException extends AppException {
    public MarketDataUnavailableException(String message) {
        super("MARKET_DATA_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
