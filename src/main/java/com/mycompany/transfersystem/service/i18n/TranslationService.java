package com.mycompany.transfersystem.service.i18n;

import com.mycompany.transfersystem.entity.Translation;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.TranslationRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranslationService {

    private final TranslationRepository translationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final StringRedisTemplate redisTemplate;

    public String getMessage(String key, String locale) {
        String normalizedLocale = normalizeLocale(locale);
        String cacheKey = "i18n:" + normalizedLocale + ":" + key;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) return cached;
        } catch (Exception e) {
            log.debug("Redis unavailable for i18n cache, computing directly");
        }
        String value = resolveMessage(key, normalizedLocale);
        try {
            redisTemplate.opsForValue().set(cacheKey, value, Duration.ofSeconds(3600));
        } catch (Exception e) {
            log.debug("Redis unavailable, skipping cache write");
        }
        return value;
    }

    @Transactional
    public void upsertTranslation(String locale, String key, String value, Long editorId) {
        String normalizedLocale = normalizeLocale(locale);
        Translation t = translationRepository.findByLocaleAndMessageKey(normalizedLocale, key)
                .orElse(Translation.builder().locale(normalizedLocale).messageKey(key).build());
        t.setMessageValue(value);
        translationRepository.save(t);
        try {
            redisTemplate.delete("i18n:" + normalizedLocale + ":" + key);
            redisTemplate.delete("i18n:bundle:" + normalizedLocale);
        } catch (Exception ignored) {}
        User editor = editorId == null ? null : userRepository.findById(editorId).orElse(null);
        auditService.log("TRANSLATION_UPSERTED", "TRANSLATION", t.getId(), normalizedLocale + ":" + key, editor);
    }

    @Transactional(readOnly = true)
    public Map<String, String> loadBundle(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        String bundleCacheKey = "i18n:bundle:" + normalizedLocale;
        try {
            Map<Object, Object> cached = redisTemplate.opsForHash().entries(bundleCacheKey);
            if (!cached.isEmpty()) {
                return cached.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
            }
        } catch (Exception ignored) {}
        Map<String, String> bundle = new LinkedHashMap<>();
        List<String> fallbackChain = candidateLocales(normalizedLocale);
        for (int i = fallbackChain.size() - 1; i >= 0; i--) {
            translationRepository.findAllByLocale(fallbackChain.get(i))
                    .forEach(t -> bundle.put(t.getMessageKey(), t.getMessageValue()));
        }
        try {
            if (!bundle.isEmpty()) {
                redisTemplate.opsForHash().putAll(bundleCacheKey, bundle);
                redisTemplate.expire(bundleCacheKey, Duration.ofSeconds(3600));
            }
        } catch (Exception ignored) {}
        return bundle;
    }

    public String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "ar-SY";
        }
        String cleaned = locale.trim().replace('_', '-');
        String[] parts = cleaned.split("-");
        if (parts.length == 1) {
            return parts[0].toLowerCase(Locale.ROOT);
        }
        return parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT);
    }

    private String resolveMessage(String key, String locale) {
        return candidateLocales(locale).stream()
                .map(candidate -> translationRepository.findByLocaleAndMessageKey(candidate, key))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(Translation::getMessageValue)
                .findFirst()
                .orElse(key);
    }

    private List<String> candidateLocales(String locale) {
        String normalized = normalizeLocale(locale);
        if ("ar-SY".equals(normalized)) {
            return List.of("ar-SY", "ar", "en");
        }
        if (normalized.startsWith("ar-")) {
            return List.of(normalized, "ar-SY", "ar", "en");
        }
        if (normalized.contains("-")) {
            return List.of(normalized, normalized.substring(0, normalized.indexOf('-')), "en");
        }
        if ("en".equals(normalized)) {
            return List.of("en");
        }
        return List.of(normalized, "en");
    }
}
