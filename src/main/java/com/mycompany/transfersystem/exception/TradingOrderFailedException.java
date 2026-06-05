package com.mycompany.transfersystem.exception;

import com.mycompany.transfersystem.entity.Order;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

public class TradingOrderFailedException extends AppException {

    public TradingOrderFailedException(Order order) {
        super("TRADING_ORDER_FAILED", HttpStatus.BAD_REQUEST,
                order.getFailureDetail() != null ? order.getFailureDetail() : "Trading order was not executed",
                details(order));
    }

    private static Map<String, Object> details(Order order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", order.getId());
        if (order.getFailureReason() != null) {
            m.put("failureReason", order.getFailureReason().name());
        }
        m.put("status", order.getStatus().name());
        return m;
    }
}
