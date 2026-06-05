package com.mycompany.transfersystem.dto.idempotency;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Persisted shape for idempotent HTTP replays (status + JSON body tree).
 */
public record IdempotentHttpEnvelope(int httpStatus, JsonNode body) {
}
