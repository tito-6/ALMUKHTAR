package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.FeeBreakdownDTO;
import com.mycompany.transfersystem.dto.TransactionFeeRequest;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.CommissionRate;
import com.mycompany.transfersystem.entity.Currency;
import com.mycompany.transfersystem.entity.enums.CommissionScope;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.CommissionRateRepository;
import com.mycompany.transfersystem.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FeeCalculationServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CommissionRateRepository commissionRateRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private FeeCalculationService feeCalculationService;

    private Branch mainAdminBranch;
    private Branch branchA;
    private Branch branchB;
    private Currency usdCurrency;
    private Currency tlCurrency;

    @BeforeEach
    void setUp() {
        // Setup branches
        mainAdminBranch = new Branch();
        mainAdminBranch.setId(1L);
        mainAdminBranch.setName("MAIN_ADMIN_BRANCH");

        branchA = new Branch();
        branchA.setId(2L);
        branchA.setName("BRANCH_A");

        branchB = new Branch();
        branchB.setId(3L);
        branchB.setName("BRANCH_B");

        // Setup currencies
        usdCurrency = new Currency();
        usdCurrency.setCode("USD");
        usdCurrency.setExchangeRateToUsd(BigDecimal.ONE);

        tlCurrency = new Currency();
        tlCurrency.setCode("TL");
        tlCurrency.setExchangeRateToUsd(new BigDecimal("0.0241")); // 1 TL = 0.0241 USD (1 USD = 41.45 TL)
    }

    @Test
    public void testNonUsdToUsdTransaction_AppliesExchangeProfit() {
        // Arrange: 41,450 TL → $1,000 USD equivalent transaction
        TransactionFeeRequest request = new TransactionFeeRequest();
        request.setAmount(new BigDecimal("41450.00")); // 41,450 TL
        request.setSourceCurrency("TL");
        request.setDestinationCurrency("USD");
        request.setSenderBranchId(2L); // BRANCH_A
        request.setReceiverBranchId(3L); // BRANCH_B

        // Mock currency lookup
        when(currencyRepository.findByCodeAndIsActiveTrue("TL")).thenReturn(Optional.of(tlCurrency));

        // Mock branch lookups
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branchA));
        when(branchRepository.findById(3L)).thenReturn(Optional.of(branchB));
        when(branchRepository.findFirstByName("MAIN_ADMIN_BRANCH")).thenReturn(Optional.of(mainAdminBranch));

        // Mock commission rates - using the default rates from DataInitializer
        when(commissionRateRepository.findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE))
                .thenReturn(Optional.of(createCommissionRate(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE, new BigDecimal("1.50"))));
        
        when(commissionRateRepository.findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_EXCHANGE_PROFIT))
                .thenReturn(Optional.of(createCommissionRate(mainAdminBranch, CommissionScope.PLATFORM_EXCHANGE_PROFIT, new BigDecimal("1.50"))));
        
        when(commissionRateRepository.findByBranchAndCommissionScope(branchA, CommissionScope.SENDING_BRANCH_FEE))
                .thenReturn(Optional.of(createCommissionRate(branchA, CommissionScope.SENDING_BRANCH_FEE, new BigDecimal("1.50"))));
        
        when(commissionRateRepository.findByBranchAndCommissionScope(branchB, CommissionScope.RECEIVING_BRANCH_FEE))
                .thenReturn(Optional.of(createCommissionRate(branchB, CommissionScope.RECEIVING_BRANCH_FEE, new BigDecimal("4.00"))));

        // Act
        FeeBreakdownDTO result = feeCalculationService.calculateTransactionFees(request);

        // Assert
        // USD equivalent: 41,450 TL * 0.0241 = 998.945 USD ≈ 1000 USD (1 unit of 1000)
        assertThat(result.getUsdEquivalent()).isEqualByComparingTo("998.945");

        // Expected fees for 1 unit of 1000 USD:
        assertThat(result.getPlatformBaseFee()).isEqualByComparingTo("1.50");
        assertThat(result.getPlatformExchangeProfit()).isEqualByComparingTo("1.50"); // Applied because TL ≠ USD
        assertThat(result.getSendingBranchFee()).isEqualByComparingTo("1.50");
        assertThat(result.getReceivingBranchFee()).isEqualByComparingTo("4.00");
        
        // Total: $1.50 + $1.50 + $1.50 + $4.00 = $8.50
        assertThat(result.getTotalFee()).isEqualByComparingTo("8.50");
    }

    @Test
    public void testUsdToUsdTransaction_NoExchangeProfit() {
        // Arrange: USD to USD transaction (no exchange profit)
        TransactionFeeRequest request = new TransactionFeeRequest();
        request.setAmount(new BigDecimal("1000.00")); // $1,000 USD
        request.setSourceCurrency("USD");
        request.setDestinationCurrency("USD");
        request.setSenderBranchId(2L); // BRANCH_A
        request.setReceiverBranchId(3L); // BRANCH_B

        // Mock currency lookup
        when(currencyRepository.findByCodeAndIsActiveTrue("USD")).thenReturn(Optional.of(usdCurrency));

        // Mock branch lookups
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branchA));
        when(branchRepository.findById(3L)).thenReturn(Optional.of(branchB));
        when(branchRepository.findFirstByName("MAIN_ADMIN_BRANCH")).thenReturn(Optional.of(mainAdminBranch));

        // Mock commission rates
        when(commissionRateRepository.findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE))
                .thenReturn(Optional.of(createCommissionRate(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE, new BigDecimal("1.50"))));
        
        when(commissionRateRepository.findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_EXCHANGE_PROFIT))
                .thenReturn(Optional.of(createCommissionRate(mainAdminBranch, CommissionScope.PLATFORM_EXCHANGE_PROFIT, new BigDecimal("1.50"))));
        
        when(commissionRateRepository.findByBranchAndCommissionScope(branchA, CommissionScope.SENDING_BRANCH_FEE))
                .thenReturn(Optional.of(createCommissionRate(branchA, CommissionScope.SENDING_BRANCH_FEE, new BigDecimal("1.50"))));
        
        when(commissionRateRepository.findByBranchAndCommissionScope(branchB, CommissionScope.RECEIVING_BRANCH_FEE))
                .thenReturn(Optional.of(createCommissionRate(branchB, CommissionScope.RECEIVING_BRANCH_FEE, new BigDecimal("4.00"))));

        // Act
        FeeBreakdownDTO result = feeCalculationService.calculateTransactionFees(request);

        // Assert
        assertThat(result.getUsdEquivalent()).isEqualByComparingTo("1000.00");

        // Expected fees for 1 unit of 1000 USD:
        assertThat(result.getPlatformBaseFee()).isEqualByComparingTo("1.50");
        assertThat(result.getPlatformExchangeProfit()).isEqualByComparingTo("0.00"); // NOT applied because USD = USD
        assertThat(result.getSendingBranchFee()).isEqualByComparingTo("1.50");
        assertThat(result.getReceivingBranchFee()).isEqualByComparingTo("4.00");
        
        // Total: $1.50 + $0.00 + $1.50 + $4.00 = $7.00
        assertThat(result.getTotalFee()).isEqualByComparingTo("7.00");
    }

    @Test
    public void testLargeTransaction_MultipleThousandUnits() {
        // Arrange: 5,000 USD transaction (5 units of 1000)
        TransactionFeeRequest request = new TransactionFeeRequest();
        request.setAmount(new BigDecimal("5000.00")); // $5,000 USD
        request.setSourceCurrency("USD");
        request.setDestinationCurrency("EUR");
        request.setSenderBranchId(2L); // BRANCH_A
        request.setReceiverBranchId(3L); // BRANCH_B

        // Mock currency lookup
        when(currencyRepository.findByCodeAndIsActiveTrue("USD")).thenReturn(Optional.of(usdCurrency));

        // Mock branch lookups
        when(branchRepository.findById(2L)).thenReturn(Optional.of(branchA));
        when(branchRepository.findById(3L)).thenReturn(Optional.of(branchB));
        when(branchRepository.findFirstByName("MAIN_ADMIN_BRANCH")).thenReturn(Optional.of(mainAdminBranch));

        // Mock commission rates
        when(commissionRateRepository.findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE))
                .thenReturn(Optional.of(createCommissionRate(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE, new BigDecimal("1.50"))));
        
        when(commissionRateRepository.findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_EXCHANGE_PROFIT))
                .thenReturn(Optional.of(createCommissionRate(mainAdminBranch, CommissionScope.PLATFORM_EXCHANGE_PROFIT, new BigDecimal("1.50"))));
        
        when(commissionRateRepository.findByBranchAndCommissionScope(branchA, CommissionScope.SENDING_BRANCH_FEE))
                .thenReturn(Optional.of(createCommissionRate(branchA, CommissionScope.SENDING_BRANCH_FEE, new BigDecimal("1.50"))));
        
        when(commissionRateRepository.findByBranchAndCommissionScope(branchB, CommissionScope.RECEIVING_BRANCH_FEE))
                .thenReturn(Optional.of(createCommissionRate(branchB, CommissionScope.RECEIVING_BRANCH_FEE, new BigDecimal("4.00"))));

        // Act
        FeeBreakdownDTO result = feeCalculationService.calculateTransactionFees(request);

        // Assert
        assertThat(result.getUsdEquivalent()).isEqualByComparingTo("5000.00");

        // Expected fees for 5 units of 1000 USD:
        assertThat(result.getPlatformBaseFee()).isEqualByComparingTo("7.50"); // 5 * $1.50
        assertThat(result.getPlatformExchangeProfit()).isEqualByComparingTo("7.50"); // 5 * $1.50 (USD ≠ EUR)
        assertThat(result.getSendingBranchFee()).isEqualByComparingTo("7.50"); // 5 * $1.50
        assertThat(result.getReceivingBranchFee()).isEqualByComparingTo("20.00"); // 5 * $4.00
        
        // Total: $7.50 + $7.50 + $7.50 + $20.00 = $42.50
        assertThat(result.getTotalFee()).isEqualByComparingTo("42.50");
    }

    private CommissionRate createCommissionRate(Branch branch, CommissionScope scope, BigDecimal rate) {
        CommissionRate commissionRate = new CommissionRate();
        commissionRate.setBranch(branch);
        commissionRate.setCommissionScope(scope);
        commissionRate.setRateValue(rate);
        return commissionRate;
    }
}
