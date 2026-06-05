package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class TransactionNotFoundException extends AppException {
    public TransactionNotFoundException(String message) {
        super("TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }
}
