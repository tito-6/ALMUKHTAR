package com.mycompany.transfersystem.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.entity.NotificationOutbox;
import com.mycompany.transfersystem.entity.enums.NotificationOutboxStatus;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.NotificationOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationOutboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxService.class);

    private final NotificationOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public NotificationOutboxService(NotificationOutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long enqueue(NotificationCommand command, String correlationIdOverride) {
        String correlationId = correlationIdOverride != null && !correlationIdOverride.isBlank()
                ? correlationIdOverride
                : NotificationCommand.correlationId(
                command.eventType(),
                command.entityType(),
                command.entityId(),
                command.recipientUserId(),
                command.channel(),
                command.templateKey());
        return enqueueWithCorrelation(command, correlationId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long enqueue(NotificationCommand command) {
        return enqueue(command, null);
    }

    private Long enqueueWithCorrelation(NotificationCommand command, String correlationId) {
        if (repository.existsByCorrelationId(correlationId)) {
            return repository.findByCorrelationId(correlationId).map(NotificationOutbox::getId).orElse(null);
        }
        String payloadJson;
        try {
            Map<String, String> payload = new HashMap<>();
            if (command.payloadVariables() != null) {
                payload.putAll(command.payloadVariables());
            }
            if (command.structuredEventType() != null) {
                payload.put("__eventType", command.structuredEventType().name());
            }
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize notification payload", e);
        }
        NotificationOutbox row = NotificationOutbox.builder()
                .eventType(command.eventType())
                .entityType(command.entityType())
                .entityId(command.entityId())
                .recipientUserId(command.recipientUserId())
                .recipientRole(command.recipientRole())
                .channel(command.channel())
                .phoneMasked(command.phoneMasked())
                .recipientPhoneE164(command.recipientPhoneE164())
                .templateKey(command.templateKey())
                .languageCode(command.languageCode() != null ? command.languageCode() : "ar")
                .payloadJson(payloadJson)
                .status(NotificationOutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(Instant.now())
                .correlationId(correlationId)
                .build();
        try {
            return repository.save(row).getId();
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            log.debug("notification outbox duplicate correlation {}", correlationId);
            return repository.findByCorrelationId(correlationId).map(NotificationOutbox::getId).orElse(null);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueAll(List<NotificationCommand> commands) {
        for (NotificationCommand c : commands) {
            try {
                enqueue(c);
            } catch (Exception e) {
                log.warn("enqueue failed: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public void markSent(Long id, String providerMessageId) {
        repository.findById(id).ifPresent(o -> {
            o.setStatus(NotificationOutboxStatus.SENT);
            o.setProcessedAt(Instant.now());
            o.setLastError(null);
            repository.save(o);
        });
    }

    @Transactional
    public void markFailed(Long id, String error, Instant nextAttemptAt, int maxAttempts) {
        repository.findById(id).ifPresent(o -> {
            int next = o.getAttempts() + 1;
            o.setLastError(truncate(error, 2000));
            if (next >= maxAttempts) {
                o.setStatus(NotificationOutboxStatus.DEAD_LETTER);
                o.setAttempts(next);
                o.setProcessedAt(Instant.now());
            } else {
                o.setStatus(NotificationOutboxStatus.FAILED);
                o.setAttempts(next);
                o.setNextAttemptAt(nextAttemptAt);
            }
            repository.save(o);
        });
    }

    @Transactional
    public void markDeadLetter(Long id, String error) {
        repository.findById(id).ifPresent(o -> {
            o.setStatus(NotificationOutboxStatus.DEAD_LETTER);
            o.setAttempts(o.getAttempts() + 1);
            o.setLastError(truncate(error, 2000));
            o.setProcessedAt(Instant.now());
            repository.save(o);
        });
    }

    @Transactional
    public void markSkipped(Long id, String reason) {
        repository.findById(id).ifPresent(o -> {
            o.setStatus(NotificationOutboxStatus.SKIPPED);
            o.setLastError(truncate(reason, 2000));
            o.setProcessedAt(Instant.now());
            repository.save(o);
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationOutbox> listDeadLetters(int limit) {
        return repository.findByStatusOrderByCreatedAtDesc(
                NotificationOutboxStatus.DEAD_LETTER,
                PageRequest.of(0, Math.max(1, Math.min(limit, 200))));
    }

    @Transactional
    public NotificationOutbox replayDeadLetter(Long id) {
        NotificationOutbox row = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification outbox row not found: " + id));
        if (row.getStatus() != NotificationOutboxStatus.DEAD_LETTER) {
            throw new IllegalStateException("Only dead-letter notifications can be replayed");
        }
        row.setStatus(NotificationOutboxStatus.FAILED);
        row.setNextAttemptAt(Instant.now());
        row.setProcessedAt(null);
        row.setLastError(null);
        return repository.save(row);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
