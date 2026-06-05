package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.entity.PriceAlert;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.PriceAlertKind;
import com.mycompany.transfersystem.repository.PriceAlertRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceAlertServiceWhatsAppTest {

    @Mock private PriceAlertRepository priceAlertRepository;
    @Mock private UserRepository userRepository;
    @Mock private TradableAssetResolutionService tradableAssetResolutionService;
    @Mock private PriceCacheService priceCacheService;
    @Mock private YahooFinanceService yahooFinanceService;
    @Mock private TradingOutcomeNotificationService tradingOutcomeNotificationService;

    @InjectMocks private PriceAlertService priceAlertService;

    @Test
    void abovePrice_triggersOnceAndMarksWhatsAppSent() {
        PriceAlert alert = PriceAlert.builder()
                .id(10L)
                .userId(5L)
                .symbol("AAPL")
                .kind(PriceAlertKind.ABOVE_PRICE)
                .thresholdPrice(new BigDecimal("140"))
                .notifyInApp(true)
                .notifyWhatsApp(true)
                .triggered(false)
                .whatsappSent(false)
                .build();

        User user = new User();
        user.setId(5L);
        user.setPhone("+963930000000");

        when(priceAlertRepository.findByTriggeredIsFalse()).thenReturn(List.of(alert));
        when(priceCacheService.get("AAPL")).thenReturn(new BigDecimal("150"));
        when(userRepository.findById(5L)).thenReturn(java.util.Optional.of(user));
        when(priceAlertRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        priceAlertService.evaluateOpenAlerts();

        assertTrue(alert.isTriggered());
        assertTrue(alert.isWhatsappSent());
        verify(tradingOutcomeNotificationService, times(1)).notifyFromPriceAlert(eq(user), eq(alert), anyString());
        verify(priceAlertRepository, atLeastOnce()).save(alert);
    }
}
