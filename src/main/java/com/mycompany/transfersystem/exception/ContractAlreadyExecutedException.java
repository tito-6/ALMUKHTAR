package com.mycompany.transfersystem.exception;

import org.springframework.http.HttpStatus;

public class ContractAlreadyExecutedException extends AppException {
    public ContractAlreadyExecutedException(String message) {
        super("CONTRACT_ALREADY_EXECUTED", HttpStatus.CONFLICT, message);
    }
}
