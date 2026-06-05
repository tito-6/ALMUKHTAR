package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends AppException {
    public IdempotencyConflictException(String errorCode, String message) {
        super(errorCode, HttpStatus.CONFLICT, message);
    }
}
