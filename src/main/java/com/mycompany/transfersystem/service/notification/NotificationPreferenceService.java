package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.entity.UserNotificationPreference;
import com.mycompany.transfersystem.entity.enums.NotificationEventType;
import com.mycompany.transfersystem.entity.enums.NotificationMessageCategory;
import com.mycompany.transfersystem.repository.UserNotificationPreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationPreferenceService {

    private final UserNotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceService(UserNotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * @return empty if delivery is allowed; otherwise a stable skip reason code for logs.
     */
    public Optional<String> evaluateWhatsappSkipReason(Long userId, NotificationEventType eventType) {
        if (userId == null) {
            return Optional.empty();
        }
        UserNotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElse(null);
        if (pref == null) {
            return Optional.empty();
        }
        if (!pref.isWhatsappEnabled()) {
            return Optional.of("whatsapp_disabled");
        }
        if (eventType.getMessageCategory() == NotificationMessageCategory.MARKETING && !pref.isMarketingEnabled()) {
            return Optional.of("marketing_opt_out");
        }
        if (eventType.respectsTransactionAlertToggle() && !pref.isTransactionAlertsEnabled()) {
            return Optional.of("transaction_alerts_disabled");
        }
        return Optional.empty();
    }

    public String resolveLanguage(Long userId) {
        if (userId == null) {
            return "AR";
        }
        return preferenceRepository.findByUserId(userId)
                .map(p -> {
                    if (p.getLanguagePreference() == null || p.getLanguagePreference().isBlank()) {
                        return "AR";
                    }
                    return p.getLanguagePreference().toUpperCase();
                })
                .orElse("AR");
    }
}
