package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.Translation;
import com.mycompany.transfersystem.repository.TranslationRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.i18n.TranslationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock private TranslationRepository translationRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private StringRedisTemplate redisTemplate;

    @InjectMocks private TranslationService translationService;

    @Test
    void normalizeLocale_acceptsSyrianArabicAliases() {
        assertEquals("ar-SY", translationService.normalizeLocale("ar_sy"));
        assertEquals("ar-SY", translationService.normalizeLocale("AR-sy"));
        assertEquals("ar-SY", translationService.normalizeLocale(null));
    }

    @Test
    void getMessage_syrianArabicFallsBackBeforeGenericArabicAndEnglish() {
        when(translationRepository.findByLocaleAndMessageKey("ar-MA", "transfer.completed"))
                .thenReturn(Optional.empty());
        when(translationRepository.findByLocaleAndMessageKey("ar-SY", "transfer.completed"))
                .thenReturn(Optional.of(translation("ar-SY", "transfer.completed", "تمت الحوالة بنجاح.")));

        String message = translationService.getMessage("transfer.completed", "ar-MA");

        assertEquals("تمت الحوالة بنجاح.", message);
    }

    @Test
    void loadBundle_mergesEnglishGenericArabicAndSyrianOverrides() {
        when(translationRepository.findAllByLocale("en")).thenReturn(List.of(
                translation("en", "common.direction", "ltr"),
                translation("en", "transfer.completed", "Transfer completed successfully."),
                translation("en", "wallet.title", "My wallet")
        ));
        when(translationRepository.findAllByLocale("ar")).thenReturn(List.of(
                translation("ar", "common.direction", "rtl"),
                translation("ar", "transfer.completed", "تمت الحوالة بنجاح.")
        ));
        when(translationRepository.findAllByLocale("ar-SY")).thenReturn(List.of(
                translation("ar-SY", "wallet.title", "محفظتي")
        ));

        Map<String, String> bundle = translationService.loadBundle("ar_SY");

        assertEquals("rtl", bundle.get("common.direction"));
        assertEquals("تمت الحوالة بنجاح.", bundle.get("transfer.completed"));
        assertEquals("محفظتي", bundle.get("wallet.title"));
    }

    private Translation translation(String locale, String key, String value) {
        return Translation.builder()
                .locale(locale)
                .messageKey(key)
                .messageValue(value)
                .build();
    }
}
