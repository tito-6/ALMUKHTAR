package com.mycompany.transfersystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "almukhtar.notifications")
public class NotificationThresholdProperties {

    /** Amount in same currency as transfer to treat as high-value platform summary. */
    private BigDecimal transferHighValue = new BigDecimal("50000");
    private BigDecimal merchantPerTradePlatformNotify = new BigDecimal("25000");
    private BigDecimal walletTopupBranchManagerAlert = new BigDecimal("20000");
    private BigDecimal batchHighVolumeUsd = new BigDecimal("100000");

    public BigDecimal getTransferHighValue() {
        return transferHighValue;
    }

    public void setTransferHighValue(BigDecimal transferHighValue) {
        this.transferHighValue = transferHighValue;
    }

    public BigDecimal getMerchantPerTradePlatformNotify() {
        return merchantPerTradePlatformNotify;
    }

    public void setMerchantPerTradePlatformNotify(BigDecimal merchantPerTradePlatformNotify) {
        this.merchantPerTradePlatformNotify = merchantPerTradePlatformNotify;
    }

    public BigDecimal getWalletTopupBranchManagerAlert() {
        return walletTopupBranchManagerAlert;
    }

    public void setWalletTopupBranchManagerAlert(BigDecimal walletTopupBranchManagerAlert) {
        this.walletTopupBranchManagerAlert = walletTopupBranchManagerAlert;
    }

    public BigDecimal getBatchHighVolumeUsd() {
        return batchHighVolumeUsd;
    }

    public void setBatchHighVolumeUsd(BigDecimal batchHighVolumeUsd) {
        this.batchHighVolumeUsd = batchHighVolumeUsd;
    }
}
