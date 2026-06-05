package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class TenantNotFoundException extends AppException {
    public TenantNotFoundException(String message) {
        super("TENANT_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }
}
