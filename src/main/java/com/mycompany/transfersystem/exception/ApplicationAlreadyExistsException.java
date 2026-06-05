package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class ApplicationAlreadyExistsException extends AppException {
    public ApplicationAlreadyExistsException(String message) {
        super("APPLICATION_ALREADY_EXISTS", HttpStatus.CONFLICT, message);
    }
}
