package com.mycompany.transfersystem.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "almukhtar.whatsapp")
public class AlmukhtarWhatsAppProperties {

    private boolean enabled = true;
    private String provider = "mock";
    private String graphBaseUrl = "https://graph.facebook.com/v20.0";
    private String phoneNumberId = "";
    private String businessAccountId = "";
    private String accessToken = "";
    private String webhookVerifyToken = "";
    private String webhookAppSecret = "";
    private String defaultLanguage = "ar";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getGraphBaseUrl() {
        return graphBaseUrl;
    }

    public void setGraphBaseUrl(String graphBaseUrl) {
        this.graphBaseUrl = graphBaseUrl;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getBusinessAccountId() {
        return businessAccountId;
    }

    public void setBusinessAccountId(String businessAccountId) {
        this.businessAccountId = businessAccountId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getWebhookVerifyToken() {
        return webhookVerifyToken;
    }

    public void setWebhookVerifyToken(String webhookVerifyToken) {
        this.webhookVerifyToken = webhookVerifyToken;
    }

    public String getWebhookAppSecret() {
        return webhookAppSecret;
    }

    public void setWebhookAppSecret(String webhookAppSecret) {
        this.webhookAppSecret = webhookAppSecret;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
}
