package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.entity.NotificationTemplate;
import com.mycompany.transfersystem.entity.enums.NotificationChannel;
import com.mycompany.transfersystem.entity.enums.NotificationMessageCategory;
import com.mycompany.transfersystem.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppTemplateServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    private WhatsAppTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new WhatsAppTemplateService(templateRepository);
    }

    @Test
    void rendersPlaceholdersInOrder() {
        NotificationTemplate tpl = NotificationTemplate.builder()
                .templateKey("transfer_sender_completed")
                .locale("EN")
                .bodyTemplate("ID {{transactionId}} amt {{amount}} {{currency}} code {{pickupCode}}")
                .channel(NotificationChannel.WHATSAPP)
                .messageCategory(NotificationMessageCategory.OPERATIONAL)
                .build();
        when(templateRepository.findByTemplateKeyAndLocaleIgnoreCaseAndChannel(
                ArgumentMatchers.eq("transfer_sender_completed"),
                ArgumentMatchers.eq("EN"),
                ArgumentMatchers.eq(NotificationChannel.WHATSAPP)))
                .thenReturn(Optional.of(tpl));

        String out = templateService.renderBody("transfer_sender_completed", "EN",
                Map.of("transactionId", "99", "amount", "100", "currency", "USD", "pickupCode", "******"));

        assertThat(out).isEqualTo("ID 99 amt 100 USD code ******");
    }

    @Test
    void orderedPlaceholderKeysPreservesDeclarationOrder() {
        var keys = templateService.orderedPlaceholderKeys("{{a}} {{b}} {{a}}");
        assertThat(keys).containsExactly("a", "b");
    }
}
