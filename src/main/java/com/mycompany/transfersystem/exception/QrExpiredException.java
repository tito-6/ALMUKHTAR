package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class QrExpiredException extends AppException {
    public QrExpiredException(String message) {
        super("QR_EXPIRED", HttpStatus.BAD_REQUEST, message);
    }
}
