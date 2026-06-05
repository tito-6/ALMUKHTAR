package com.mycompany.transfersystem.dto.idempotency;

import org.springframework.http.ResponseEntity;

/**
 * Normalized controller return for idempotency persistence and replay.
 */
public record IdempotentExecutionResult(int httpStatus, Object body) {

    public static IdempotentExecutionResult from(Object controllerReturn) {
        if (controllerReturn instanceof ResponseEntity<?> re) {
            return new IdempotentExecutionResult(re.getStatusCode().value(), re.getBody());
        }
        return new IdempotentExecutionResult(200, controllerReturn);
    }

    public Object toMvcReturnValue() {
        return ResponseEntity.status(httpStatus).body(body);
    }
}
