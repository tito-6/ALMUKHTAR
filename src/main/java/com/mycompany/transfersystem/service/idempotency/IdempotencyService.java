package com.mycompany.transfersystem.service.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.dto.idempotency.IdempotentExecutionResult;
import com.mycompany.transfersystem.dto.idempotency.IdempotentHttpEnvelope;
import com.mycompany.transfersystem.entity.IdempotencyRecord;
import com.mycompany.transfersystem.entity.enums.IdempotencyRecordStatus;
import com.mycompany.transfersystem.exception.IdempotencyConflictException;
import com.mycompany.transfersystem.exception.IdempotencyReplayException;
import com.mycompany.transfersystem.exception.IdempotencyRequiredException;
import com.mycompany.transfersystem.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.function.Supplier;

/**
 * Enforces single execution of money-moving requests keyed by (user, endpoint, idempotency key).
 *
 * <ul>
 *   <li>Same key + same request hash, already COMPLETED → returns the stored response.</li>
 *   <li>Same key + different request hash → {@link IdempotencyConflictException} (HTTP 409).</li>
 *   <li>Concurrent IN_PROGRESS → {@link IdempotencyConflictException} (HTTP 409).</li>
 *   <li>Previous attempt FAILED → safe to retry under the same key.</li>
 * </ul>
 */
@Service
public class IdempotencyService {

    private static final int DEFAULT_TTL_DAYS = 7;

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public static String sha256Hex(String body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Transactional
    public IdempotentExecutionResult execute(long userId,
                                             String endpoint,
                                             String idempotencyKey,
                                             String requestHash,
                                             Supplier<IdempotentExecutionResult> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IdempotencyRequiredException("Idempotency-Key header is required");
        }
        Instant now = Instant.now();
        Instant expires = now.plus(DEFAULT_TTL_DAYS, ChronoUnit.DAYS);

        IdempotencyRecord existing = repository
                .findByUserIdAndEndpointAndIdempotencyKey(userId, endpoint, idempotencyKey)
                .orElse(null);

        if (existing != null) {
            switch (existing.getStatus()) {
                case IN_PROGRESS -> throw new IdempotencyConflictException("IDEMPOTENCY_IN_PROGRESS",
                        "A request with this Idempotency-Key is already being processed");
                case COMPLETED -> {
                    requireSameRequest(existing, requestHash);
                    return replay(existing);
                }
                case FAILED -> {
                    requireSameRequest(existing, requestHash);
                    existing.setStatus(IdempotencyRecordStatus.IN_PROGRESS);
                    existing.setExpiresAt(expires);
                    existing.setCompletedAt(null);
                    existing.setResponseBody(null);
                    existing.setResponseHash(null);
                    existing.setHttpStatus(null);
                    repository.save(existing);
                    return run(existing, action);
                }
            }
        }

        IdempotencyRecord row = IdempotencyRecord.builder()
                .userId(userId)
                .endpoint(endpoint)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(IdempotencyRecordStatus.IN_PROGRESS)
                .expiresAt(expires)
                .createdAt(now)
                .build();
        repository.save(row);
        return run(row, action);
    }

    private void requireSameRequest(IdempotencyRecord existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("IDEMPOTENCY_BODY_MISMATCH",
                    "The same Idempotency-Key was used with a different request");
        }
    }

    private IdempotentExecutionResult run(IdempotencyRecord row, Supplier<IdempotentExecutionResult> action) {
        try {
            IdempotentExecutionResult result = action.get();
            IdempotentHttpEnvelope envelope = new IdempotentHttpEnvelope(
                    result.httpStatus(),
                    objectMapper.valueToTree(result.body()));
            String json = objectMapper.writeValueAsString(envelope);
            row.setStatus(IdempotencyRecordStatus.COMPLETED);
            row.setCompletedAt(Instant.now());
            row.setResponseBody(json);
            row.setResponseHash(sha256Hex(json));
            row.setHttpStatus(result.httpStatus());
            repository.save(row);
            return result;
        } catch (RuntimeException | Error e) {
            row.setStatus(IdempotencyRecordStatus.FAILED);
            row.setCompletedAt(Instant.now());
            repository.save(row);
            throw e;
        } catch (Exception e) {
            row.setStatus(IdempotencyRecordStatus.FAILED);
            row.setCompletedAt(Instant.now());
            repository.save(row);
            throw new IllegalStateException("Idempotent response serialization failed", e);
        }
    }

    private IdempotentExecutionResult replay(IdempotencyRecord existing) {
        try {
            IdempotentHttpEnvelope envelope = objectMapper.readValue(existing.getResponseBody(), IdempotentHttpEnvelope.class);
            JsonNode body = envelope.body();
            Object payload = (body == null || body.isNull()) ? null : body;
            return new IdempotentExecutionResult(envelope.httpStatus(), payload);
        } catch (Exception e) {
            throw new IdempotencyReplayException("Stored idempotent response could not be replayed");
        }
    }
}
