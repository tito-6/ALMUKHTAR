package com.mycompany.transfersystem.service.i18n;

import com.mycompany.transfersystem.service.ai.AlmukhtarAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "almukhtar.ai.enabled", havingValue = "true", matchIfMissing = false)
public class AITranslationService {

    private final TranslationService translationService;
    private final AlmukhtarAiService aiService;

    @Async("notificationExecutor")
    public void autoTranslate(String messageKey, String sourceLocale, List<String> targetLocales) {
        String sourceText = translationService.getMessage(messageKey, sourceLocale);
        for (String targetLocale : targetLocales) {
            try {
                String prompt = "Translate the following text from " + sourceLocale + " to " + targetLocale + ": " + sourceText;
                String translated = aiService.chat(prompt);
                translationService.upsertTranslation(targetLocale, messageKey, translated, null);
                log.info("Auto-translated {} to {}", messageKey, targetLocale);
            } catch (Exception e) {
                log.warn("Failed to auto-translate {} to {}: {}", messageKey, targetLocale, e.getMessage());
            }
        }
    }
}
