package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cashier_shift_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"shift_id", "currency"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashierShiftBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private CashierShift shift;

    @Column(nullable = false, length = 10)
    private String currency;

    @Builder.Default
    @Column(name = "opening_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cash_in_total", nullable = false, precision = 20, scale = 4)
    private BigDecimal cashInTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cash_out_total", nullable = false, precision = 20, scale = 4)
    private BigDecimal cashOutTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "expected_closing_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal expectedClosingBalance = BigDecimal.ZERO;

    @Column(name = "actual_closing_balance", precision = 20, scale = 4)
    private BigDecimal actualClosingBalance;

    @Column(precision = 20, scale = 4)
    private BigDecimal variance;
}
