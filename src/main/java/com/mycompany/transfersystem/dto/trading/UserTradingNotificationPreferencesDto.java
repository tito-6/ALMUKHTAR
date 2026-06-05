package com.mycompany.transfersystem.dto.trading;

import lombok.Data;

@Data
public class UserTradingNotificationPreferencesDto {
    private boolean orderInApp = true;
    private boolean orderWhatsApp;
    private boolean priceAlertInApp = true;
    private boolean priceAlertWhatsApp;
}
