package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.entity.InAppNotification;
import com.mycompany.transfersystem.entity.Order;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.UserTradingNotificationPreferences;
import com.mycompany.transfersystem.entity.enums.OrderStatus;
import com.mycompany.transfersystem.repository.InAppNotificationRepository;
import com.mycompany.transfersystem.repository.UserTradingNotificationPreferencesRepository;
import com.mycompany.transfersystem.service.notification.NotificationDispatchService;
import org.springframework.stereotype.Service;

@Service
public class TradingOutcomeNotificationService {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final UserTradingNotificationPreferencesRepository preferencesRepository;
    private final NotificationDispatchService notificationDispatchService;

    public TradingOutcomeNotificationService(InAppNotificationRepository inAppNotificationRepository,
                                             UserTradingNotificationPreferencesRepository preferencesRepository,
                                             NotificationDispatchService notificationDispatchService) {
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.preferencesRepository = preferencesRepository;
        this.notificationDispatchService = notificationDispatchService;
    }

    public void notifyOrderOutcome(User user, Order order) {
        UserTradingNotificationPreferences p = preferencesRepository.findByUserId(user.getId()).orElse(null);
        boolean inApp = p == null || p.isOrderInApp();
        boolean wa = p != null && p.isOrderWhatsApp();

        String title = order.getStatus() == OrderStatus.FILLED ? "Trade executed" : "Trade not executed";
        StringBuilder body = new StringBuilder();
        body.append(order.getSymbol()).append(" ").append(order.getSide()).append(" qty ").append(order.getQuantity());
        if (order.getStatus() == OrderStatus.FILLED) {
            body.append(" @ ").append(order.getFilledPrice()).append(" fee ").append(order.getPlatformFee());
        } else if (order.getFailureReason() != null) {
            body.append(" — ").append(order.getFailureReason());
            if (order.getFailureDetail() != null) {
                body.append(": ").append(order.getFailureDetail());
            }
        }

        if (inApp) {
            inAppNotificationRepository.save(InAppNotification.builder()
                    .userId(user.getId())
                    .title(title)
                    .body(body.toString())
                    .type(InAppNotification.NotificationType.ALERT)
                    .build());
        }
        if (wa) {
            notificationDispatchService.dispatchTradingOutcomeWhatsapp(user, order.getStatus(), title, body.toString());
        }
    }

    /**
     * Delivers a triggered price alert respecting per-alert toggles and user-level preferences.
     */
    public void notifyFromPriceAlert(User user, com.mycompany.transfersystem.entity.PriceAlert alert, String detail) {
        UserTradingNotificationPreferences p = preferencesRepository.findByUserId(user.getId()).orElse(null);
        boolean inApp = alert.isNotifyInApp() && (p == null || p.isPriceAlertInApp());
        boolean wa = alert.isNotifyWhatsApp() && (p != null && p.isPriceAlertWhatsApp());

        if (inApp) {
            inAppNotificationRepository.save(InAppNotification.builder()
                    .userId(user.getId())
                    .title("Price alert: " + alert.getSymbol())
                    .body(detail)
                    .type(InAppNotification.NotificationType.ALERT)
                    .build());
        }
        if (wa) {
            notificationDispatchService.dispatchTradingPriceAlertWhatsapp(user, alert.getSymbol(), detail);
        }
    }
}
