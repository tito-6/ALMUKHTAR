package com.mycompany.transfersystem.exception;

public class AccountingImbalanceException extends RuntimeException {

    public AccountingImbalanceException(String message) {
        super(message);
    }

    public AccountingImbalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
