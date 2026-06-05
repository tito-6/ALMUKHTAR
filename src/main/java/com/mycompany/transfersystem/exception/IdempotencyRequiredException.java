package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyRequiredException extends AppException {
    public IdempotencyRequiredException(String message) {
        super("IDEMPOTENCY_KEY_REQUIRED", HttpStatus.BAD_REQUEST, message);
    }
}
