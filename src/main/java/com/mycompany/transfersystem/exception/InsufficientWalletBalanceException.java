package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class InsufficientWalletBalanceException extends AppException {
    public InsufficientWalletBalanceException(String message) {
        super("INSUFFICIENT_WALLET_BALANCE", HttpStatus.BAD_REQUEST, message);
    }
}
