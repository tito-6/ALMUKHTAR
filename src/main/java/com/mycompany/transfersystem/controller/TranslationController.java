package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.i18n.AITranslationService;
import com.mycompany.transfersystem.service.i18n.TranslationService;
import com.mycompany.transfersystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/i18n")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private AITranslationService aiTranslationService;

    @GetMapping("/bundle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> bundle(@RequestParam String locale) {
        return ResponseEntity.ok(translationService.loadBundle(locale));
    }

    @PutMapping("/translations")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<Void> upsert(@RequestParam String locale, @RequestParam String key,
                                         @RequestParam String value,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        translationService.upsertTranslation(locale, key, value, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/translations/auto-translate")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<Void> autoTranslate(@RequestParam String messageKey,
                                                @RequestParam String sourceLocale,
                                                @RequestParam List<String> targetLocales) {
        if (aiTranslationService == null) {
            return ResponseEntity.status(501).build();
        }
        aiTranslationService.autoTranslate(messageKey, sourceLocale, targetLocales);
        return ResponseEntity.accepted().build();
    }
}
