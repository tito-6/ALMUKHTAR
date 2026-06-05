package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class ConditionNotMetException extends AppException {
    public ConditionNotMetException(String message) {
        super("CONDITION_NOT_MET", HttpStatus.BAD_REQUEST, message);
    }
}
