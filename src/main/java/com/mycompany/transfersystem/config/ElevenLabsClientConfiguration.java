package com.mycompany.transfersystem.config;

import com.mycompany.transfersystem.config.properties.ElevenLabsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(ElevenLabsProperties.class)
public class ElevenLabsClientConfiguration {

    @Bean(name = "elevenLabsWebClient")
    @ConditionalOnProperty(name = "almukhtar.elevenlabs.enabled", havingValue = "true")
    public WebClient elevenLabsWebClient(ElevenLabsProperties properties) {
        String base = properties.getBaseUrl() != null ? properties.getBaseUrl() : "https://api.elevenlabs.io";
        WebClient.Builder b = WebClient.builder()
                .baseUrl(base)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            b = b.defaultHeader("xi-api-key", properties.getApiKey());
        }
        return b.build();
    }
}
