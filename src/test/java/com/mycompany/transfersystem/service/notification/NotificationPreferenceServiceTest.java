package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.entity.UserNotificationPreference;
import com.mycompany.transfersystem.entity.enums.NotificationEventType;
import com.mycompany.transfersystem.repository.UserNotificationPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private UserNotificationPreferenceRepository preferenceRepository;

    @Test
    void skipsWhenWhatsappDisabled() {
        NotificationPreferenceService svc = new NotificationPreferenceService(preferenceRepository);
        UserNotificationPreference p = UserNotificationPreference.builder()
                .userId(1L)
                .whatsappEnabled(false)
                .marketingEnabled(true)
                .transactionAlertsEnabled(true)
                .build();
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(p));

        assertThat(svc.evaluateWhatsappSkipReason(1L, NotificationEventType.transfer_sender_completed))
                .contains("whatsapp_disabled");
    }

    @Test
    void marketingOptOutBlocksMaintenanceTemplate() {
        NotificationPreferenceService svc = new NotificationPreferenceService(preferenceRepository);
        UserNotificationPreference p = UserNotificationPreference.builder()
                .userId(2L)
                .whatsappEnabled(true)
                .marketingEnabled(false)
                .transactionAlertsEnabled(true)
                .build();
        when(preferenceRepository.findByUserId(2L)).thenReturn(Optional.of(p));

        assertThat(svc.evaluateWhatsappSkipReason(2L, NotificationEventType.system_maintenance))
                .contains("marketing_opt_out");
    }

    @Test
    void securityStyleEventsBypassTransactionToggle() {
        NotificationPreferenceService svc = new NotificationPreferenceService(preferenceRepository);
        UserNotificationPreference p = UserNotificationPreference.builder()
                .userId(3L)
                .whatsappEnabled(true)
                .marketingEnabled(true)
                .transactionAlertsEnabled(false)
                .build();
        when(preferenceRepository.findByUserId(3L)).thenReturn(Optional.of(p));

        assertThat(svc.evaluateWhatsappSkipReason(3L, NotificationEventType.account_frozen_user_notice)).isEmpty();
    }
}
