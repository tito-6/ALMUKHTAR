package com.mycompany.transfersystem.exception;

public class UnauthorizedFeeModificationException extends RuntimeException {
    
    public UnauthorizedFeeModificationException(String message) {
        super(message);
    }
    
    public UnauthorizedFeeModificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
