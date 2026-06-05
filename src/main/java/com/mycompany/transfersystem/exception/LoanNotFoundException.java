package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class LoanNotFoundException extends AppException {
    public LoanNotFoundException(String message) {
        super("LOAN_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }
}
