package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class QrTokenNotFoundException extends AppException {
    public QrTokenNotFoundException(String message) {
        super("QR_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }
}
