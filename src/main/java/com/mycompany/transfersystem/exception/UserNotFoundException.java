package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends AppException {
    public UserNotFoundException(String message) {
        super("USER_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }
}
