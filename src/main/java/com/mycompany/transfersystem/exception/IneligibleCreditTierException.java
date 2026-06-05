package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class IneligibleCreditTierException extends AppException {
    public IneligibleCreditTierException(String message) {
        super("INELIGIBLE_CREDIT_TIER", HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
