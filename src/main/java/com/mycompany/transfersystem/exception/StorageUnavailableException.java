package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class StorageUnavailableException extends AppException {
    public StorageUnavailableException(String message) {
        super("STORAGE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
