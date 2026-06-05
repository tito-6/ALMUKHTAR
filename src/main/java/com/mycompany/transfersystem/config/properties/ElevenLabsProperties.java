package com.mycompany.transfersystem.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "almukhtar.elevenlabs")
public class ElevenLabsProperties {

    private boolean enabled;
    private String apiKey;
    private String webhookSecret;
    /**
     * API base URL, without trailing slash.
     */
    private String baseUrl = "https://api.elevenlabs.io";
    /**
     * When false, voice webhooks only persist masked transcript text, not raw audio payloads.
     */
    private boolean storeRawAudio = false;
    private final AgentIds agent = new AgentIds();

    @Data
    public static class AgentIds {
        private String customerId;
        private String cashierId;
        private String managerId;
        private String ownerId;
        private String tradingId;
    }
}
