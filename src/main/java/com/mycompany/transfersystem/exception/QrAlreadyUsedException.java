package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class QrAlreadyUsedException extends AppException {
    public QrAlreadyUsedException(String message) {
        super("QR_ALREADY_USED", HttpStatus.CONFLICT, message);
    }
}
