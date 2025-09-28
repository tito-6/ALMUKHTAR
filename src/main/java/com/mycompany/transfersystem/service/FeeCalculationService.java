package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.FeeBreakdownDTO;
import com.mycompany.transfersystem.dto.TransactionFeeRequest;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.BranchFeeRate;
import com.mycompany.transfersystem.entity.CommissionRate;
import com.mycompany.transfersystem.entity.Currency;
import com.mycompany.transfersystem.entity.enums.CommissionScope;
import com.mycompany.transfersystem.repository.BranchFeeRateRepository;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.CommissionRateRepository;
import com.mycompany.transfersystem.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeeCalculationService {

    // Flat constant per 1000 USD equivalent
    public static final BigDecimal PLATFORM_BASE_FEE_PER_1000_USD = new BigDecimal("1.50");

    private final BranchFeeRateRepository branchFeeRateRepository;
    private final CommissionRateRepository commissionRateRepository;
    private final BranchRepository branchRepository;
    private final CurrencyRepository currencyRepository;

    @Autowired
    public FeeCalculationService(BranchFeeRateRepository branchFeeRateRepository,
                                CommissionRateRepository commissionRateRepository,
                                BranchRepository branchRepository,
                                CurrencyRepository currencyRepository) {
        this.branchFeeRateRepository = branchFeeRateRepository;
        this.commissionRateRepository = commissionRateRepository;
        this.branchRepository = branchRepository;
        this.currencyRepository = currencyRepository;
    }

    // For simple unit tests that don't need repository access
    public FeeCalculationService() {
        this.branchFeeRateRepository = null;
        this.commissionRateRepository = null;
        this.branchRepository = null;
        this.currencyRepository = null;
    }

    // Convert an amount in sourceCurrency to USD equivalent using Currency.exchangeRateToUsd if present
    public BigDecimal toUsd(BigDecimal amount, Currency sourceCurrency) {
        if (amount == null || sourceCurrency == null) return BigDecimal.ZERO;
        BigDecimal rateToUsd = sourceCurrency.getExchangeRateToUsd();
        if (rateToUsd == null || rateToUsd.compareTo(BigDecimal.ZERO) <= 0) {
            // Fallback: if rate not available, assume USD for safety
            rateToUsd = BigDecimal.ONE;
        }
        return amount.multiply(rateToUsd);
    }

    // Ceiling units of 1000 USD: ceil(usd / 1000)
    private BigDecimal thousandUsdUnits(BigDecimal usdAmount) {
        if (usdAmount == null) return BigDecimal.ZERO;
        BigDecimal[] divRem = usdAmount.divideAndRemainder(new BigDecimal("1000"));
        BigDecimal units = divRem[0];
        if (divRem[1].compareTo(BigDecimal.ZERO) > 0) {
            units = units.add(BigDecimal.ONE);
        }
        return units;
    }

    // Step 2.1: Platform base fee applies to all transactions
    public BigDecimal calculatePlatformBaseFee(BigDecimal amount, Currency sourceCurrency) {
        BigDecimal usd = toUsd(amount, sourceCurrency);
        BigDecimal units = thousandUsdUnits(usd);
        return units.multiply(PLATFORM_BASE_FEE_PER_1000_USD).setScale(2, RoundingMode.HALF_UP);
    }

    // Step 2.2: Sending branch fee using configured rate ($1.00 per 1000 USD default)
    public BigDecimal calculateSendingBranchFee(BigDecimal amount, Currency sourceCurrency, Branch sendingBranch) {
        BigDecimal usd = toUsd(amount, sourceCurrency);
        BigDecimal units = thousandUsdUnits(usd);
    BigDecimal rate = new BigDecimal("1.00");
    if (branchFeeRateRepository != null && sendingBranch != null && sendingBranch.getId() != null) {
        rate = branchFeeRateRepository
            .findFirstByBranch_Id(sendingBranch.getId())
            .map(BranchFeeRate::getSendingPerThousandUsd)
            .orElse(rate);
    }
        return units.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    // Step 2.2: Receiving branch fee using configured rate ($4.00 - $7.00 per 1000 USD, default $4.00)
    public BigDecimal calculateReceivingBranchFee(BigDecimal amount, Currency sourceCurrency, Branch receivingBranch) {
        BigDecimal usd = toUsd(amount, sourceCurrency);
        BigDecimal units = thousandUsdUnits(usd);
    BigDecimal rate = new BigDecimal("4.00");
    if (branchFeeRateRepository != null && receivingBranch != null && receivingBranch.getId() != null) {
        rate = branchFeeRateRepository
            .findFirstByBranch_Id(receivingBranch.getId())
            .map(BranchFeeRate::getReceivingPerThousandUsd)
            .orElse(rate);
    }
        return units.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Main method to calculate all transaction fees using the new four-tiered system
     * @param request Transaction fee request with amount, currencies, and branch IDs
     * @return Complete fee breakdown with all four fee components
     */
    public FeeBreakdownDTO calculateTransactionFees(TransactionFeeRequest request) {
        // Get source currency
        Currency sourceCurrency = currencyRepository.findByCodeAndIsActiveTrue(request.getSourceCurrency())
                .orElseThrow(() -> new RuntimeException("Source currency not found: " + request.getSourceCurrency()));

        // Calculate USD equivalent
        BigDecimal usdEquivalent = toUsd(request.getAmount(), sourceCurrency);

        // Get branches
        Branch senderBranch = branchRepository.findById(request.getSenderBranchId())
                .orElseThrow(() -> new RuntimeException("Sender branch not found: " + request.getSenderBranchId()));
        Branch receiverBranch = branchRepository.findById(request.getReceiverBranchId())
                .orElseThrow(() -> new RuntimeException("Receiver branch not found: " + request.getReceiverBranchId()));

        // Get main admin branch for platform fees
        Branch mainAdminBranch = branchRepository.findFirstByName("MAIN_ADMIN_BRANCH")
                .orElseThrow(() -> new RuntimeException("Main admin branch not found"));

        // Calculate all four fee components
        BigDecimal platformBaseFee = calculatePlatformBaseFeeNew(usdEquivalent, mainAdminBranch);
        BigDecimal platformExchangeProfit = calculatePlatformExchangeProfit(usdEquivalent, request.getSourceCurrency(), 
                request.getDestinationCurrency(), mainAdminBranch);
        BigDecimal sendingBranchFee = calculateSendingBranchFeeNew(usdEquivalent, senderBranch);
        BigDecimal receivingBranchFee = calculateReceivingBranchFeeNew(usdEquivalent, receiverBranch);

        return new FeeBreakdownDTO(platformBaseFee, platformExchangeProfit, sendingBranchFee, receivingBranchFee, usdEquivalent);
    }

    /**
     * Calculate platform base fee using new CommissionRate system
     */
    private BigDecimal calculatePlatformBaseFeeNew(BigDecimal usdAmount, Branch mainAdminBranch) {
        BigDecimal units = thousandUsdUnits(usdAmount);
        BigDecimal rate = commissionRateRepository
                .findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_BASE_FEE)
                .map(CommissionRate::getRateValue)
                .orElse(new BigDecimal("1.50")); // Default fallback
        return units.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate platform exchange profit - only applied when currencies differ
     */
    private BigDecimal calculatePlatformExchangeProfit(BigDecimal usdAmount, String sourceCurrency, 
                                                      String destinationCurrency, Branch mainAdminBranch) {
        // Only apply exchange profit if currencies are different
        if (sourceCurrency.equals(destinationCurrency)) {
            return BigDecimal.ZERO;
        }

        BigDecimal units = thousandUsdUnits(usdAmount);
        BigDecimal rate = commissionRateRepository
                .findByBranchAndCommissionScope(mainAdminBranch, CommissionScope.PLATFORM_EXCHANGE_PROFIT)
                .map(CommissionRate::getRateValue)
                .orElse(new BigDecimal("1.50")); // Default fallback
        return units.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate sending branch fee using new CommissionRate system
     */
    private BigDecimal calculateSendingBranchFeeNew(BigDecimal usdAmount, Branch sendingBranch) {
        BigDecimal units = thousandUsdUnits(usdAmount);
        BigDecimal rate = commissionRateRepository
                .findByBranchAndCommissionScope(sendingBranch, CommissionScope.SENDING_BRANCH_FEE)
                .map(CommissionRate::getRateValue)
                .orElse(new BigDecimal("1.50")); // Default fallback
        return units.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate receiving branch fee using new CommissionRate system
     */
    private BigDecimal calculateReceivingBranchFeeNew(BigDecimal usdAmount, Branch receivingBranch) {
        BigDecimal units = thousandUsdUnits(usdAmount);
        BigDecimal rate = commissionRateRepository
                .findByBranchAndCommissionScope(receivingBranch, CommissionScope.RECEIVING_BRANCH_FEE)
                .map(CommissionRate::getRateValue)
                .orElse(new BigDecimal("4.00")); // Default fallback
        return units.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}