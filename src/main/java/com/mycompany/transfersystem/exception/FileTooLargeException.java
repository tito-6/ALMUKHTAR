package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class FileTooLargeException extends AppException {
    public FileTooLargeException(String message) {
        super("FILE_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE, message);
    }
}
