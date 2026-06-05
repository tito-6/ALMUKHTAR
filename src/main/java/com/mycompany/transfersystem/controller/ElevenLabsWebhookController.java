package com.mycompany.transfersystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.config.properties.ElevenLabsProperties;
import com.mycompany.transfersystem.dto.ai.ElevenLabsWebhookPayload;
import com.mycompany.transfersystem.entity.AiConversationMessage;
import com.mycompany.transfersystem.entity.AiConversationSession;
import com.mycompany.transfersystem.repository.AiConversationMessageRepository;
import com.mycompany.transfersystem.repository.AiConversationSessionRepository;
import com.mycompany.transfersystem.service.ai.AiDataMaskingUtil;
import com.mycompany.transfersystem.service.ai.ElevenLabsWebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks/elevenlabs")
@RequiredArgsConstructor
public class ElevenLabsWebhookController {

    private final ElevenLabsProperties elevenLabsProperties;
    private final ObjectMapper objectMapper;
    private final AiConversationSessionRepository sessionRepository;
    private final AiConversationMessageRepository messageRepository;

    @PostMapping(value = "/conversation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> conversation(
            @RequestBody String rawBody,
            @RequestHeader(value = "ElevenLabs-Signature", required = false) String signatureHeader) {
        if (!elevenLabsProperties.isEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!ElevenLabsWebhookSignatureVerifier.isValid(rawBody, signatureHeader, elevenLabsProperties.getWebhookSecret())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            ElevenLabsWebhookPayload payload = objectMapper.readValue(rawBody, ElevenLabsWebhookPayload.class);
            String convId = firstNonBlank(
                    payload.getConversationId(),
                    textAt(payload.getData(), "conversation_id"),
                    textAt(payload.getMetadata(), "conversation_id"));
            if (convId == null) {
                return ResponseEntity.ok().build();
            }
            Optional<AiConversationSession> sessionOpt = sessionRepository.findByExternalConversationId(convId);
            if (sessionOpt.isEmpty()) {
                return ResponseEntity.ok().build();
            }
            AiConversationSession session = sessionOpt.get();
            JsonNode transcript = payload.getTranscript();
            if (transcript != null && transcript.isArray()) {
                for (JsonNode line : transcript) {
                    String role = textAt(line, "role");
                    String message = textAt(line, "message");
                    if (message == null) {
                        message = line.toString();
                    }
                    if (!elevenLabsProperties.isStoreRawAudio() && message != null && message.contains("audio")) {
                        message = "[audio omitted]";
                    }
                    String direction = mapRole(role);
                    String masked = AiDataMaskingUtil.maskPhonesAndIds(message);
                    messageRepository.save(AiConversationMessage.builder()
                            .session(session)
                            .direction(direction)
                            .channel(AiConversationMessage.CHANNEL_VOICE)
                            .contentMasked(masked)
                            .build());
                }
            } else if (rawBody.length() < 8000) {
                messageRepository.save(AiConversationMessage.builder()
                        .session(session)
                        .direction(AiConversationMessage.DIRECTION_SYSTEM)
                        .channel(AiConversationMessage.CHANNEL_VOICE)
                        .contentMasked(AiDataMaskingUtil.maskPhonesAndIds(rawBody))
                        .build());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok().build();
    }

    private static String mapRole(String role) {
        if (role == null) {
            return AiConversationMessage.DIRECTION_SYSTEM;
        }
        return switch (role.toLowerCase()) {
            case "user", "customer" -> AiConversationMessage.DIRECTION_USER;
            case "agent", "assistant", "ai" -> AiConversationMessage.DIRECTION_ASSISTANT;
            default -> AiConversationMessage.DIRECTION_SYSTEM;
        };
    }

    private static String textAt(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return null;
        }
        JsonNode v = node.get(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText(null);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
