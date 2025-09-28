package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.BranchFeeRate;
import com.mycompany.transfersystem.entity.Currency;
import com.mycompany.transfersystem.repository.BranchFeeRateRepository;
import com.mycompany.transfersystem.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(FeeCalculationService.class)
class BranchFeeTest {

    @Autowired
    private BranchFeeRateRepository branchFeeRateRepository;

    @Autowired
    private FeeCalculationService feeCalculationService;

    @Autowired
    private BranchRepository branchRepository;

    private Currency USD;
    private Currency EUR;

    @BeforeEach
    void setup() {
        USD = new Currency();
        USD.setCode("USD");
        USD.setExchangeRateToUsd(new BigDecimal("1.00"));

        EUR = new Currency();
        EUR.setCode("EUR");
        EUR.setExchangeRateToUsd(new BigDecimal("1.10"));
    }

    @Test
    void testSendingAndReceivingBranchFees_DefaultRates() {
        Branch send = new Branch();
        send.setName("SendBranch");
        Branch recv = new Branch();
        recv.setName("RecvBranch");

        // No BranchFeeRate records saved -> use defaults: 1.00 sending, 4.00 receiving
        BigDecimal amount = new BigDecimal("1000");
        BigDecimal sending = feeCalculationService.calculateSendingBranchFee(amount, USD, send);
        BigDecimal receiving = feeCalculationService.calculateReceivingBranchFee(amount, USD, recv);

        assertEquals(new BigDecimal("1.00"), sending);
        assertEquals(new BigDecimal("4.00"), receiving);
    }

    @Test
    void testSendingAndReceivingBranchFees_ConfiguredRatesAndCeilingUnits() {
    Branch send = new Branch();
    send.setName("SendConfigured");
    Branch recv = new Branch();
    recv.setName("RecvConfigured");
    send = branchRepository.save(send);
    recv = branchRepository.save(recv);

        // Persist fee rates
    BranchFeeRate sRate = new BranchFeeRate();
    sRate.setBranch(send);
        sRate.setSendingPerThousandUsd(new BigDecimal("1.25"));
        sRate.setReceivingPerThousandUsd(new BigDecimal("5.00")); // unused for sending
        branchFeeRateRepository.save(sRate);

    BranchFeeRate rRate = new BranchFeeRate();
    rRate.setBranch(recv);
        rRate.setSendingPerThousandUsd(new BigDecimal("1.00")); // unused for receiving
        rRate.setReceivingPerThousandUsd(new BigDecimal("6.50"));
        branchFeeRateRepository.save(rRate);

        // EUR 1001 -> 1101.10 USD -> ceil to 2 units
        BigDecimal amount = new BigDecimal("1001");
        BigDecimal sending = feeCalculationService.calculateSendingBranchFee(amount, EUR, send);
        BigDecimal receiving = feeCalculationService.calculateReceivingBranchFee(amount, EUR, recv);

        assertEquals(new BigDecimal("2.50"), sending);   // 2 * 1.25
        assertEquals(new BigDecimal("13.00"), receiving); // 2 * 6.50
    }
}
