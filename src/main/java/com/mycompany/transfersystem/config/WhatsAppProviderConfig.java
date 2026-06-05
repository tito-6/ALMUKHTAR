package com.mycompany.transfersystem.config;

import com.mycompany.transfersystem.config.properties.AlmukhtarWhatsAppProperties;
import com.mycompany.transfersystem.service.notification.provider.MetaWhatsAppProvider;
import com.mycompany.transfersystem.service.notification.provider.MockWhatsAppProvider;
import com.mycompany.transfersystem.service.notification.provider.WhatsAppProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AlmukhtarWhatsAppProperties.class)
public class WhatsAppProviderConfig {

    @Bean
    @ConditionalOnMissingBean
    public WhatsAppProvider whatsAppProvider(WebClient.Builder webClientBuilder,
                                             AlmukhtarWhatsAppProperties alProps,
                                             org.springframework.core.env.Environment env) {
        String providerType = firstNonBlank(alProps.getProvider(),
                env.getProperty("whatsapp.provider.type", "mock"));
        String baseUrl = firstNonBlank(alProps.getGraphBaseUrl(),
                env.getProperty("whatsapp.api.base-url", "https://graph.facebook.com/v20.0"));
        String phoneNumberId = firstNonBlank(alProps.getPhoneNumberId(),
                env.getProperty("whatsapp.api.phone-number-id", ""));
        String accessToken = firstNonBlank(alProps.getAccessToken(),
                env.getProperty("whatsapp.api.access-token", ""));

        if ("meta".equalsIgnoreCase(providerType) && StringUtils.hasText(accessToken) && StringUtils.hasText(phoneNumberId)) {
            WebClient client = webClientBuilder.baseUrl(trimTrailingSlash(baseUrl)).build();
            return new MetaWhatsAppProvider(client, phoneNumberId, accessToken);
        }
        return new MockWhatsAppProvider();
    }

    private static String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }

    private static String trimTrailingSlash(String u) {
        if (u.endsWith("/")) {
            return u.substring(0, u.length() - 1);
        }
        return u;
    }
}
