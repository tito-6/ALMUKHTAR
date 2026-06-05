package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class WalletFrozenException extends AppException {
    public WalletFrozenException(String message) {
        super("WALLET_FROZEN", HttpStatus.FORBIDDEN, message);
    }
}
