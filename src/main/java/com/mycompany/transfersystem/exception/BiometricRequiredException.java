package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class BiometricRequiredException extends AppException {
    public BiometricRequiredException(String message) {
        super("BIOMETRIC_REQUIRED", HttpStatus.FORBIDDEN, message);
    }
}
