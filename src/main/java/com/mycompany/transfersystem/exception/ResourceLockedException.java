package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class ResourceLockedException extends AppException {
    public ResourceLockedException(String message) {
        super("RESOURCE_LOCKED", HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
