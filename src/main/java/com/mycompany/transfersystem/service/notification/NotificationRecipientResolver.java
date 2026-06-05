package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.notification.FinancialNotificationDispatch;
import com.mycompany.transfersystem.notification.NotificationRecipientPolicy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade over {@link NotificationRecipientPolicy} for role-based routing (sender, receiver, cashier, platform, etc.).
 */
@Service
public class NotificationRecipientResolver {

    private final NotificationRecipientPolicy policy;

    public NotificationRecipientResolver(NotificationRecipientPolicy policy) {
        this.policy = policy;
    }

    public List<FinancialNotificationDispatch> resolveTransferCreated(FinancialWorkflowEvents.TransferCreatedEvent e) {
        return policy.forTransferCreated(e);
    }

    public List<FinancialNotificationDispatch> resolveTransferReady(FinancialWorkflowEvents.TransferReadyForPickupEvent e) {
        return policy.forTransferReadyForPickup(e);
    }

    public List<FinancialNotificationDispatch> resolveTransferReleased(FinancialWorkflowEvents.TransferReleasedEvent e) {
        return policy.forTransferReleased(e);
    }
}
