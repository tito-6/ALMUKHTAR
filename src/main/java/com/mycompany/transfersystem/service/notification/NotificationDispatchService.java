package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.entity.NotificationDeliveryLog;
import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.NotificationChannel;
import com.mycompany.transfersystem.entity.enums.NotificationDeliveryStatus;
import com.mycompany.transfersystem.entity.enums.NotificationEventType;
import com.mycompany.transfersystem.entity.enums.OrderStatus;
import com.mycompany.transfersystem.notification.FinancialWhatsAppMapper;
import com.mycompany.transfersystem.notification.NotificationTemplateKeys;
import com.mycompany.transfersystem.repository.NotificationDeliveryLogRepository;
import com.mycompany.transfersystem.repository.TransactionRepository;
import com.mycompany.transfersystem.service.notification.util.NotificationPhoneMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private static final String ENTITY_TRANSACTION = "Transaction";
    private static final String ENTITY_USER = "User";
    private static final String ENTITY_BRANCH = "Branch";

    private final TransactionRepository transactionRepository;
    private final NotificationDeliveryLogWriter logWriter;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final NotificationPreferenceService preferenceService;
    private final WhatsAppTemplateService templateService;
    private final NotificationOutboxService notificationOutboxService;

    public NotificationDispatchService(TransactionRepository transactionRepository,
                                       NotificationDeliveryLogWriter logWriter,
                                       NotificationDeliveryLogRepository deliveryLogRepository,
                                       NotificationPreferenceService preferenceService,
                                       WhatsAppTemplateService templateService,
                                       NotificationOutboxService notificationOutboxService) {
        this.transactionRepository = transactionRepository;
        this.logWriter = logWriter;
        this.deliveryLogRepository = deliveryLogRepository;
        this.preferenceService = preferenceService;
        this.templateService = templateService;
        this.notificationOutboxService = notificationOutboxService;
    }

    public void dispatchFinancialWhatsapp(Long userId, String phone, String templateKey, String headline, String detail) {
        if (userId == null) {
            throw new IllegalArgumentException("userId required for financial WhatsApp dispatch");
        }
        NotificationEventType eventType = FinancialWhatsAppMapper.eventTypeForTemplateKey(templateKey);
        Map<String, String> vars = new HashMap<>();
        vars.put("headline", headline != null ? headline : "");
        vars.put("detail", detail != null ? detail : "");
        vars.put("referenceId", String.valueOf(userId));
        sendWhatsappLeg(userId, phone, eventType, templateKey, ENTITY_USER, userId, vars);
    }

    public void dispatchBranchOperationalWhatsapp(String phone, Long branchId, String headline, String detail) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        NotificationEventType eventType = FinancialWhatsAppMapper.eventTypeForTemplateKey("internal.branch.ops");
        Map<String, String> vars = new HashMap<>();
        vars.put("headline", headline != null ? headline : "");
        vars.put("detail", detail != null ? detail : "");
        vars.put("referenceId", String.valueOf(branchId));
        sendWhatsappLeg(null, phone, eventType, "internal.branch.ops", ENTITY_BRANCH, branchId, vars);
    }

    public void dispatchTradingOutcomeWhatsapp(User user,
                                               OrderStatus status,
                                               String title,
                                               String body) {
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            return;
        }
        String templateKey = status == OrderStatus.FILLED
                ? NotificationTemplateKeys.TRADING_ORDER_FILLED
                : NotificationTemplateKeys.TRADING_ORDER_FAILED;
        dispatchFinancialWhatsapp(user.getId(), user.getPhone(), templateKey, title, body);
    }

    public void dispatchTradingPriceAlertWhatsapp(User user,
                                                  String symbol,
                                                  String detail) {
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            return;
        }
        dispatchFinancialWhatsapp(user.getId(), user.getPhone(), "trading.price.alert",
                "Price alert: " + symbol, detail);
    }

    public void adminRetryDelivery(Long deliveryLogId) {
        NotificationDeliveryLog logEntry = deliveryLogRepository.findById(deliveryLogId)
                .orElseThrow(() -> new IllegalArgumentException("delivery log not found"));
        if (logEntry.getStatus() != NotificationDeliveryStatus.FAILED
                && logEntry.getStatus() != NotificationDeliveryStatus.PENDING_RETRY) {
            throw new IllegalStateException("Only FAILED or PENDING_RETRY deliveries can be retried");
        }
        if (!ENTITY_TRANSACTION.equals(logEntry.getEntityType()) || logEntry.getEntityId() == null || logEntry.getUserId() == null) {
            throw new IllegalStateException("Retry only supported for transaction deliveries with user id");
        }
        Optional<Transaction> txOpt = transactionRepository.findByIdWithParticipants(logEntry.getEntityId());
        if (txOpt.isEmpty()) {
            throw new IllegalStateException("Transaction no longer exists");
        }
        Transaction tx = txOpt.get();
        User user = tx.getSender().getId().equals(logEntry.getUserId()) ? tx.getSender()
                : tx.getReceiver().getId().equals(logEntry.getUserId()) ? tx.getReceiver() : null;
        if (user == null) {
            throw new IllegalStateException("User on log does not match transaction parties");
        }
        String phone = user.getPhone();
        if (phone == null || phone.isBlank()) {
            throw new IllegalStateException("User has no phone on file");
        }
        Optional<String> skip = preferenceService.evaluateWhatsappSkipReason(user.getId(), logEntry.getEventType());
        if (skip.isPresent()) {
            throw new IllegalStateException("User preferences block delivery: " + skip.get());
        }
        templateService.findTemplate(logEntry.getTemplateKey(), preferenceService.resolveLanguage(user.getId()))
                .orElseThrow(() -> new IllegalStateException("Missing template " + logEntry.getTemplateKey()));

        Map<String, String> vars = baseTransferVariables(tx);
        String language = preferenceService.resolveLanguage(user.getId());
        String masked = NotificationPhoneMask.mask(phone);
        String e164 = NotificationPhoneMask.toE164Digits(phone);

        String correlation = com.mycompany.transfersystem.service.idempotency.IdempotencyService.sha256Hex(
                "admin_retry|DeliveryLog|" + deliveryLogId + "|" + logEntry.getTemplateKey());

        logEntry.setStatus(NotificationDeliveryStatus.PENDING);
        logEntry.setFailureReason(null);
        logEntry.setProviderMessageId(null);
        logEntry.setSentAt(null);
        logEntry.setDeliveredAt(null);
        logEntry.setReadAt(null);
        logWriter.saveUpdate(logEntry);

        notificationOutboxService.enqueue(new NotificationCommand(
                "admin_retry",
                ENTITY_TRANSACTION,
                tx.getId(),
                user.getId(),
                null,
                NotificationChannel.WHATSAPP.name(),
                masked,
                e164,
                logEntry.getTemplateKey(),
                language,
                vars,
                logEntry.getEventType()
        ), correlation);
    }

    private Map<String, String> baseTransferVariables(Transaction tx) {
        Map<String, String> vars = new HashMap<>();
        vars.put("transactionId", String.valueOf(tx.getId()));
        vars.put("amount", tx.getAmount().toPlainString());
        vars.put("currency", tx.getCurrencyCode() != null ? tx.getCurrencyCode() : "USD");
        vars.put("pickupCode", tx.getReleasePasscode() != null ? tx.getReleasePasscode() : "");
        vars.put("senderName", tx.getSender().getUsername());
        vars.put("receiverName", tx.getReceiver().getUsername());
        vars.put("transactionCreatedAt", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "");
        return vars;
    }

    private void sendWhatsappLeg(Long userId,
                                 String phone,
                                 NotificationEventType eventType,
                                 String templateKey,
                                 String entityType,
                                 Long entityId,
                                 Map<String, String> variables) {
        String masked = NotificationPhoneMask.mask(phone);
        String language = preferenceService.resolveLanguage(userId);

        if (phone == null || phone.isBlank()) {
            log.debug("WhatsApp enqueue skipped (no phone) event={} template={}", eventType, templateKey);
            return;
        }

        Optional<String> skip = preferenceService.evaluateWhatsappSkipReason(userId, eventType);
        if (skip.isPresent()) {
            log.debug("WhatsApp enqueue skipped ({}) event={} template={}", skip.get(), eventType, templateKey);
            return;
        }

        if (templateService.findTemplate(templateKey, language).isEmpty()) {
            log.warn("WhatsApp enqueue skipped (missing_template) template={} lang={}", templateKey, language);
            return;
        }

        String e164 = NotificationPhoneMask.toE164Digits(phone);
        notificationOutboxService.enqueue(new NotificationCommand(
                eventType.name(),
                entityType,
                entityId,
                userId,
                null,
                NotificationChannel.WHATSAPP.name(),
                masked,
                e164,
                templateKey,
                language,
                variables,
                eventType
        ));
    }
}
