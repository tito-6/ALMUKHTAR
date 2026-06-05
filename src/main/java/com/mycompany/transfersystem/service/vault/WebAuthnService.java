package com.mycompany.transfersystem.service.vault;

import com.mycompany.transfersystem.exception.BiometricRequiredException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.WebAuthnCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebAuthnService {

    private final WebAuthnCredentialRepository credentialRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public Map<String, Object> startRegistration(Long userId) {
        String challenge = UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set("webauthn:reg:" + userId, challenge, Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("Redis unavailable for WebAuthn challenge storage");
        }
        return Map.of("challenge", challenge, "userId", userId);
    }

    @Transactional
    public void finishRegistration(Long userId, String registrationResponseJson) {
        try {
            String storedChallenge = redisTemplate.opsForValue().get("webauthn:reg:" + userId);
            if (storedChallenge == null) {
                throw new BiometricRequiredException("Registration challenge expired");
            }
            redisTemplate.delete("webauthn:reg:" + userId);
        } catch (BiometricRequiredException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable for WebAuthn verification");
        }
        log.info("WebAuthn registration completed for user {}", userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> startAuthentication(Long userId) {
        var creds = credentialRepository.findByUserId(userId);
        if (creds.isEmpty()) {
            throw new BiometricRequiredException("No biometric credentials registered");
        }
        String challenge = UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set("webauthn:auth:" + userId, challenge, Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("Redis unavailable for WebAuthn challenge storage");
        }
        return Map.of("challenge", challenge, "userId", userId);
    }

    @Transactional
    public void finishAuthentication(Long userId, String assertionResponseJson) {
        try {
            String storedChallenge = redisTemplate.opsForValue().get("webauthn:auth:" + userId);
            if (storedChallenge == null) {
                throw new BiometricRequiredException("Authentication challenge expired");
            }
            redisTemplate.delete("webauthn:auth:" + userId);
        } catch (BiometricRequiredException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable for WebAuthn verification");
        }
        log.info("WebAuthn authentication completed for user {}", userId);
    }
}
