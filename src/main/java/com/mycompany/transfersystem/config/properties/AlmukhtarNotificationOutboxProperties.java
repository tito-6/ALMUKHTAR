package com.mycompany.transfersystem.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "almukhtar.notifications.outbox")
public class AlmukhtarNotificationOutboxProperties {

    private boolean enabled = true;
    private int batchSize = 50;
    private long fixedDelayMs = 5000;
    private int maxAttempts = 5;
    private long initialBackoffSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getInitialBackoffSeconds() {
        return initialBackoffSeconds;
    }

    public void setInitialBackoffSeconds(long initialBackoffSeconds) {
        this.initialBackoffSeconds = initialBackoffSeconds;
    }
}
