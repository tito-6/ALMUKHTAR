package com.mycompany.transfersystem.dto.trading;

import com.mycompany.transfersystem.entity.enums.OrderSide;
import com.mycompany.transfersystem.entity.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlaceOrderRequest {

    @NotNull
    private String symbol;

    @NotNull
    private OrderSide side;

    @NotNull
    private OrderType orderType;

    @NotNull
    @DecimalMin("0.000001")
    private BigDecimal quantity;

    private BigDecimal limitPrice;
}
