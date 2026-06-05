package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends AppException {
    public InsufficientFundsException(String message) {
        super("INSUFFICIENT_FUNDS", HttpStatus.BAD_REQUEST, message);
    }
}
