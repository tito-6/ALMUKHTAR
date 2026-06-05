package com.mycompany.transfersystem.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.mycompany.transfersystem.config.properties.ElevenLabsProperties;
import com.mycompany.transfersystem.entity.enums.AiAgentRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@Slf4j
public class ElevenLabsAgentService {

    private final ElevenLabsProperties properties;
    private final WebClient elevenLabsWebClient;

    public ElevenLabsAgentService(
            ElevenLabsProperties properties,
            @Autowired(required = false) @Qualifier("elevenLabsWebClient") WebClient elevenLabsWebClient) {
        this.properties = properties;
        this.elevenLabsWebClient = elevenLabsWebClient;
    }

    public Optional<String> resolveAgentExternalId(AiAgentRole role) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        return switch (role) {
            case CUSTOMER_HELPER -> Optional.ofNullable(emptyToNull(properties.getAgent().getCustomerId()));
            case CASHIER_COPILOT -> Optional.ofNullable(emptyToNull(properties.getAgent().getCashierId()));
            case BRANCH_MANAGER_COPILOT -> Optional.ofNullable(emptyToNull(properties.getAgent().getManagerId()));
            case PLATFORM_OWNER_COPILOT -> Optional.ofNullable(emptyToNull(properties.getAgent().getOwnerId()));
            case TRADING_ASSISTANT -> Optional.ofNullable(emptyToNull(properties.getAgent().getTradingId()));
        };
    }

    /**
     * Calls ElevenLabs ConvAI signed URL endpoint when WebClient is configured.
     */
    public Mono<JsonNode> fetchSignedConversationPayload(AiAgentRole role) {
        WebClient wc = elevenLabsWebClient;
        if (wc == null || !properties.isEnabled()) {
            return Mono.empty();
        }
        Optional<String> agentId = resolveAgentExternalId(role);
        if (agentId.isEmpty()) {
            return Mono.empty();
        }
        String uri = UriComponentsBuilder.fromPath("/v1/convai/conversation/get-signed-url")
                .queryParam("agent_id", agentId.get())
                .build()
                .toUriString();
        return wc.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.warn("ElevenLabs ConvAI request failed: {}", e.toString()))
                .onErrorResume(e -> Mono.empty());
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
