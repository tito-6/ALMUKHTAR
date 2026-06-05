package com.mycompany.transfersystem.service.trading;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PriceAlertScheduler {

    private final PriceAlertService priceAlertService;

    public PriceAlertScheduler(PriceAlertService priceAlertService) {
        this.priceAlertService = priceAlertService;
    }

    @Scheduled(fixedDelayString = "${almukhtar.trading.price-alert-ms:60000}")
    public void pollPriceAlerts() {
        priceAlertService.evaluateOpenAlerts();
    }
}
