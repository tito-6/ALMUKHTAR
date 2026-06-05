package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.ratelimit.RateLimitService;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.vault.WebAuthnService;
import com.mycompany.transfersystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/biometric")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BiometricController {

    private final WebAuthnService webAuthnService;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;

    @PostMapping("/register/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> startRegistration(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(webAuthnService.startRegistration(user.getId()));
    }

    @PostMapping("/register/finish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> finishRegistration(@RequestBody String responseJson,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        webAuthnService.finishRegistration(user.getId(), responseJson);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> startAuth(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(webAuthnService.startAuthentication(user.getId()));
    }

    @PostMapping("/auth/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> verify(@RequestBody String assertionJson,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        if (!rateLimitService.tryConsume("biometric:" + user.getId(), 5)) {
            return ResponseEntity.status(429).body("Too many requests");
        }
        webAuthnService.finishAuthentication(user.getId(), assertionJson);
        return ResponseEntity.ok().build();
    }
}
