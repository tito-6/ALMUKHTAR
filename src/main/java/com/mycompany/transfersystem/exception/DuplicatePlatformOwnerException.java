package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class DuplicatePlatformOwnerException extends AppException {
    public DuplicatePlatformOwnerException(String message) {
        super("DUPLICATE_PLATFORM_OWNER", HttpStatus.CONFLICT, message);
    }
}
