package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class FamilySpendingLimitExceededException extends AppException {
    public FamilySpendingLimitExceededException(String message) {
        super("FAMILY_SPENDING_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST, message);
    }
}
