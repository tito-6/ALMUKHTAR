package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class InvalidCsvFormatException extends AppException {
    public InvalidCsvFormatException(String message) {
        super("INVALID_CSV_FORMAT", HttpStatus.BAD_REQUEST, message);
    }
}
