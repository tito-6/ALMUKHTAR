package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class WalletNotFoundException extends AppException {
    public WalletNotFoundException(String message) {
        super("WALLET_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }
}
