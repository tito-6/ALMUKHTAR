package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class QrSignatureInvalidException extends AppException {
    public QrSignatureInvalidException(String message) {
        super("QR_SIGNATURE_INVALID", HttpStatus.UNAUTHORIZED, message);
    }
}
