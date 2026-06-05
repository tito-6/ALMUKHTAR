package com.mycompany.transfersystem.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Minimal subset of ElevenLabs post-conversation webhook payloads; extra fields ignored.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElevenLabsWebhookPayload {

    private String type;
    private String event;
    private JsonNode data;

    @JsonProperty("conversation_id")
    private String conversationId;
    private JsonNode transcript;
    private JsonNode metadata;
    private JsonNode analysis;
}
