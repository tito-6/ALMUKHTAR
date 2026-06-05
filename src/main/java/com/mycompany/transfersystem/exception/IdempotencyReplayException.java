package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyReplayException extends AppException {
    public IdempotencyReplayException(String message) {
        super("IDEMPOTENCY_REPLAY_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
