package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class UnsupportedCurrencyException extends AppException {
    public UnsupportedCurrencyException(String message) {
        super("UNSUPPORTED_CURRENCY", HttpStatus.BAD_REQUEST, message);
    }
}
