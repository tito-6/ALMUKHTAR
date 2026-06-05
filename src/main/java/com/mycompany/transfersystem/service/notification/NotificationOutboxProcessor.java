package com.mycompany.transfersystem.service.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.entity.NotificationDeliveryLog;
import com.mycompany.transfersystem.entity.NotificationOutbox;
import com.mycompany.transfersystem.entity.NotificationTemplate;
import com.mycompany.transfersystem.entity.enums.NotificationChannel;
import com.mycompany.transfersystem.entity.enums.NotificationDeliveryStatus;
import com.mycompany.transfersystem.entity.enums.NotificationEventType;
import com.mycompany.transfersystem.entity.enums.NotificationOutboxStatus;
import com.mycompany.transfersystem.notification.FinancialWhatsAppMapper;
import com.mycompany.transfersystem.repository.NotificationOutboxRepository;
import com.mycompany.transfersystem.service.notification.provider.WhatsAppProvider;
import com.mycompany.transfersystem.service.notification.provider.WhatsAppSendResult;
import com.mycompany.transfersystem.service.notification.util.NotificationPhoneMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Processes a single outbox row in an isolated transaction (one failure must not roll back others).
 */
@Service
public class NotificationOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxProcessor.class);

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationOutboxService outboxService;
    private final NotificationPreferenceService preferenceService;
    private final WhatsAppTemplateService templateService;
    private final WhatsAppProvider whatsAppProvider;
    private final NotificationDeliveryLogWriter logWriter;
    private final ObjectMapper objectMapper;

    public NotificationOutboxProcessor(NotificationOutboxRepository outboxRepository,
                                       NotificationOutboxService outboxService,
                                       NotificationPreferenceService preferenceService,
                                       WhatsAppTemplateService templateService,
                                       WhatsAppProvider whatsAppProvider,
                                       NotificationDeliveryLogWriter logWriter,
                                       ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.outboxService = outboxService;
        this.preferenceService = preferenceService;
        this.templateService = templateService;
        this.whatsAppProvider = whatsAppProvider;
        this.logWriter = logWriter;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRow(Long outboxId, int maxAttempts, long initialBackoffSeconds) {
        NotificationOutbox row = outboxRepository.findByIdForUpdate(outboxId).orElse(null);
        if (row == null || row.getStatus() == NotificationOutboxStatus.SENT
                || row.getStatus() == NotificationOutboxStatus.SKIPPED
                || row.getStatus() == NotificationOutboxStatus.DEAD_LETTER) {
            return;
        }
        if (row.getNextAttemptAt().isAfter(Instant.now())) {
            return;
        }
        if (!"WHATSAPP".equalsIgnoreCase(row.getChannel())) {
            outboxService.markSkipped(outboxId, "unsupported_channel:" + row.getChannel());
            return;
        }

        Map<String, String> variables = new HashMap<>(readVariables(row.getPayloadJson()));
        NotificationEventType eventType = parseEventType(row.getTemplateKey(), variables);

        Long userId = row.getRecipientUserId();
        Optional<String> skip = preferenceService.evaluateWhatsappSkipReason(userId, eventType);
        if (skip.isPresent()) {
            writeSkippedDeliveryLog(row, eventType, skip.get());
            outboxService.markSkipped(outboxId, skip.get());
            return;
        }

        String phoneDigits = row.getRecipientPhoneE164();
        if (phoneDigits == null || phoneDigits.isBlank()) {
            writeSkippedDeliveryLog(row, eventType, "no_phone");
            outboxService.markSkipped(outboxId, "no_phone");
            return;
        }

        String language = preferenceService.resolveLanguage(userId);
        Optional<NotificationTemplate> templateOpt = templateService.findTemplate(row.getTemplateKey(), language);
        if (templateOpt.isEmpty()) {
            logWriter.saveNew(NotificationDeliveryLog.builder()
                    .userId(userId)
                    .phoneMasked(row.getPhoneMasked() != null ? row.getPhoneMasked() : "")
                    .channel(NotificationChannel.WHATSAPP)
                    .eventType(eventType)
                    .entityType(row.getEntityType())
                    .entityId(row.getEntityId())
                    .templateKey(row.getTemplateKey())
                    .language(language)
                    .status(NotificationDeliveryStatus.FAILED)
                    .retryCount(row.getAttempts())
                    .failureReason("missing_template")
                    .build());
            failOrDeadLetter(row, maxAttempts, initialBackoffSeconds, "missing_template");
            return;
        }
        NotificationTemplate template = templateOpt.get();
        String rendered = templateService.render(template.getBodyTemplate(), variables);

        NotificationDeliveryLog deliveryLog = logWriter.saveNew(NotificationDeliveryLog.builder()
                .userId(userId)
                .phoneMasked(row.getPhoneMasked() != null ? row.getPhoneMasked() : NotificationPhoneMask.mask(phoneDigits))
                .channel(NotificationChannel.WHATSAPP)
                .eventType(eventType)
                .entityType(row.getEntityType())
                .entityId(row.getEntityId())
                .templateKey(row.getTemplateKey())
                .language(template.getLocale() != null ? template.getLocale() : language)
                .status(NotificationDeliveryStatus.PENDING)
                .retryCount(row.getAttempts())
                .build());

        WhatsAppSendResult result = invokeProvider(phoneDigits, template, rendered, variables);
        if (result.success()) {
            deliveryLog.setStatus(NotificationDeliveryStatus.SENT);
            deliveryLog.setProviderMessageId(result.providerMessageId());
            deliveryLog.setSentAt(Instant.now());
            deliveryLog.setFailureReason(null);
            logWriter.saveUpdate(deliveryLog);
            outboxService.markSent(outboxId, result.providerMessageId());
            return;
        }

        deliveryLog.setStatus(result.transientFailure() ? NotificationDeliveryStatus.PENDING_RETRY : NotificationDeliveryStatus.FAILED);
        deliveryLog.setFailureReason(truncate(result.failureDetail(), 1000));
        logWriter.saveUpdate(deliveryLog);

        failOrDeadLetter(row, maxAttempts, initialBackoffSeconds, result.failureDetail());
    }

    private void failOrDeadLetter(NotificationOutbox row, int maxAttempts, long initialBackoffSeconds, String error) {
        long backoffSec = initialBackoffSeconds * (1L << Math.min(row.getAttempts() + 1, 8));
        Instant next = Instant.now().plusSeconds(Math.min(backoffSec, 86400));
        outboxService.markFailed(row.getId(), error != null ? error : "failed", next, maxAttempts);
    }

    private void writeSkippedDeliveryLog(NotificationOutbox row, NotificationEventType eventType, String reason) {
        logWriter.saveNew(NotificationDeliveryLog.builder()
                .userId(row.getRecipientUserId())
                .phoneMasked(row.getPhoneMasked() != null ? row.getPhoneMasked() : "")
                .channel(NotificationChannel.WHATSAPP)
                .eventType(eventType)
                .entityType(row.getEntityType())
                .entityId(row.getEntityId())
                .templateKey(row.getTemplateKey())
                .language(row.getLanguageCode())
                .status(NotificationDeliveryStatus.SKIPPED)
                .retryCount(0)
                .failureReason(truncate(reason, 1000))
                .build());
    }

    private Map<String, String> readVariables(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private NotificationEventType parseEventType(String templateKey, Map<String, String> variables) {
        String raw = variables.remove("__eventType");
        if (raw != null) {
            try {
                return NotificationEventType.valueOf(raw);
            } catch (Exception ignored) {
                // fall through
            }
        }
        return FinancialWhatsAppMapper.eventTypeForTemplateKey(templateKey);
    }

    private WhatsAppSendResult invokeProvider(String phone,
                                              NotificationTemplate template,
                                              String renderedBody,
                                              Map<String, String> variables) {
        if (template.getMetaTemplateName() != null && !template.getMetaTemplateName().isBlank()) {
            List<Map<String, String>> ordered = new ArrayList<>();
            for (String key : templateService.orderedPlaceholderKeys(template.getBodyTemplate())) {
                String text = variables.getOrDefault(key, "");
                ordered.add(Map.of("type", "text", "text", text));
            }
            String lang = template.getLocale() == null ? "ar" : template.getLocale().toLowerCase();
            return whatsAppProvider.sendTemplate(phone, template.getMetaTemplateName(), lang, ordered);
        }
        return whatsAppProvider.sendText(phone, renderedBody);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
