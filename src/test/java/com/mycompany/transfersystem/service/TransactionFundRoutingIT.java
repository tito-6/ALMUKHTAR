package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.TransactionRecordDTO;
import com.mycompany.transfersystem.dto.TransferTransactionRequest;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TransactionFundRoutingIT {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User sender;
    private User receiver;
    private Fund senderFund;
    private Branch branchA;
    private Branch branchB;
    private Fund platformFund;
    private Fund branchAFund;
    private Fund branchBFund;

    @BeforeEach
    void setUp() {
        // Use existing branches from DataInitializer
        branchA = branchRepository.findFirstByName("BRANCH_A")
                .orElseThrow(() -> new RuntimeException("BRANCH_A not found"));
        branchB = branchRepository.findFirstByName("BRANCH_B")
                .orElseThrow(() -> new RuntimeException("BRANCH_B not found"));

        // Create test users
        sender = new User();
        sender.setUsername("testSender");
        sender.setPassword(passwordEncoder.encode("password123"));
        sender.setRole(UserRole.CASHIER);
        sender = userRepository.save(sender);

        receiver = new User();
        receiver.setUsername("testReceiver");
        receiver.setPassword(passwordEncoder.encode("password123"));
        receiver.setRole(UserRole.CASHIER);
        receiver = userRepository.save(receiver);

        // Create sender fund with $1,000,000 starting balance
        senderFund = new Fund();
        senderFund.setName("Sender Test Fund");
        senderFund.setBalance(new BigDecimal("1000000.00"));
        senderFund.setStatus(FundStatus.ACTIVE);
        senderFund = fundRepository.save(senderFund);

        // Create platform fund with $1,000,000 starting balance
        platformFund = new Fund();
        platformFund.setName("Platform Fund");
        platformFund.setBalance(new BigDecimal("1000000.00"));
        platformFund.setStatus(FundStatus.ACTIVE);
        platformFund = fundRepository.save(platformFund);

        // Create branch A fund with $1,000,000 starting balance
        branchAFund = new Fund();
        branchAFund.setName("BRANCH_A Fund");
        branchAFund.setBalance(new BigDecimal("1000000.00"));
        branchAFund.setStatus(FundStatus.ACTIVE);
        branchAFund = fundRepository.save(branchAFund);

        // Create branch B fund with $1,000,000 starting balance
        branchBFund = new Fund();
        branchBFund.setName("BRANCH_B Fund");
        branchBFund.setBalance(new BigDecimal("1000000.00"));
        branchBFund.setStatus(FundStatus.ACTIVE);
        branchBFund = fundRepository.save(branchBFund);
    }

    @Test
    public void testUsdToUsdTransfer_FundRouting() {
        // Test Scenario 1: USD-to-USD Transfer (No Exchange Profit)
        // Transfer $1,000 USD from Branch A to Branch B
        
        TransferTransactionRequest request = new TransferTransactionRequest();
        request.setSenderId(sender.getId());
        request.setReceiverId(receiver.getId());
        request.setFundId(branchAFund.getId());
        request.setAmount(new BigDecimal("1000.00"));
        request.setSourceCurrency("USD");
        request.setDestinationCurrency("USD");
        request.setSenderBranchId(branchA.getId());
        request.setReceiverBranchId(branchB.getId());

        // Record initial balances
        BigDecimal initialSenderFundBalance = senderFund.getBalance();
        BigDecimal initialPlatformFundBalance = platformFund.getBalance();
        BigDecimal initialBranchAFundBalance = branchAFund.getBalance();
        BigDecimal initialBranchBFundBalance = branchBFund.getBalance();

        // Execute transaction
        TransactionRecordDTO result = transactionService.executeTransfer(request);

        // Verify transaction record
        assertThat(result.getGrossAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.getTotalFees()).isEqualByComparingTo("7.00"); // $1.50 + $1.50 + $4.00
        assertThat(result.getNetAmount()).isEqualByComparingTo("1000.00"); // receiver gets clean principal
        assertThat(result.getPlatformExchangeProfit()).isEqualByComparingTo("0.00"); // No exchange profit for USD->USD

        // Refresh fund balances from database
        senderFund = fundRepository.findById(senderFund.getId()).orElseThrow();
        platformFund = fundRepository.findById(platformFund.getId()).orElseThrow();
        branchAFund = fundRepository.findById(branchAFund.getId()).orElseThrow();
        branchBFund = fundRepository.findById(branchBFund.getId()).orElseThrow();

        // Verify fund balance changes
        // Sender client fund is not the settlement account for this comprehensive transfer.
        BigDecimal expectedSenderFundChange = BigDecimal.ZERO;
        BigDecimal actualSenderFundChange = senderFund.getBalance().subtract(initialSenderFundBalance);
        assertThat(actualSenderFundChange).isEqualByComparingTo(expectedSenderFundChange);

        // Platform Fund: +$1.50 (Platform Base Fee only, no exchange profit)
        BigDecimal expectedPlatformFundChange = new BigDecimal("1.50");
        BigDecimal actualPlatformFundChange = platformFund.getBalance().subtract(initialPlatformFundBalance);
        assertThat(actualPlatformFundChange).isEqualByComparingTo(expectedPlatformFundChange);

        // Branch A/source fund pays principal plus all fees.
        BigDecimal expectedBranchAFundChange = new BigDecimal("-1007.00");
        BigDecimal actualBranchAFundChange = branchAFund.getBalance().subtract(initialBranchAFundBalance);
        assertThat(actualBranchAFundChange).isEqualByComparingTo(expectedBranchAFundChange);

        // Branch B receives full clean payout principal.
        BigDecimal expectedBranchBFundChange = new BigDecimal("1000.00");
        BigDecimal actualBranchBFundChange = branchBFund.getBalance().subtract(initialBranchBFundBalance);
        assertThat(actualBranchBFundChange).isEqualByComparingTo(expectedBranchBFundChange);
    }

    @Test
    public void testNonUsdToUsdTransfer_WithExchangeProfit() {
        // Test Scenario 2: Non-USD Transfer (With Exchange Profit)
        // Transfer 41,450 TL (=~$1,000 USD) from Branch A to Branch B, delivered in USD
        
        TransferTransactionRequest request = new TransferTransactionRequest();
        request.setSenderId(sender.getId());
        request.setReceiverId(receiver.getId());
        request.setFundId(branchAFund.getId());
        request.setAmount(new BigDecimal("41450.00")); // 41,450 TL
        request.setSourceCurrency("TL");
        request.setDestinationCurrency("USD");
        request.setSenderBranchId(branchA.getId());
        request.setReceiverBranchId(branchB.getId());

        // Record initial balances
        BigDecimal initialSenderFundBalance = senderFund.getBalance();
        BigDecimal initialPlatformFundBalance = platformFund.getBalance();
        BigDecimal initialBranchAFundBalance = branchAFund.getBalance();
        BigDecimal initialBranchBFundBalance = branchBFund.getBalance();

        // Execute transaction
        TransactionRecordDTO result = transactionService.executeTransfer(request);

        // Verify transaction record
        assertThat(result.getGrossAmount()).isEqualByComparingTo("41450.00");
        assertThat(result.getUsdEquivalent()).isEqualByComparingTo("1367.85");
        assertThat(result.getTotalFees()).isEqualByComparingTo("17.00"); // 2 units * ($1.50 + $1.50 + $1.50 + $4.00)
        assertThat(result.getNetAmount()).isEqualByComparingTo("1367.85");
        assertThat(result.getPlatformExchangeProfit()).isEqualByComparingTo("3.00"); // Exchange profit applied for TL->USD

        // Refresh fund balances from database
        senderFund = fundRepository.findById(senderFund.getId()).orElseThrow();
        platformFund = fundRepository.findById(platformFund.getId()).orElseThrow();
        branchAFund = fundRepository.findById(branchAFund.getId()).orElseThrow();
        branchBFund = fundRepository.findById(branchBFund.getId()).orElseThrow();

        // Verify fund balance changes
        BigDecimal expectedSenderFundChange = BigDecimal.ZERO;
        BigDecimal actualSenderFundChange = senderFund.getBalance().subtract(initialSenderFundBalance);
        assertThat(actualSenderFundChange).isEqualByComparingTo(expectedSenderFundChange);

        // Platform Fund: +$6.00 ($3.00 Platform Base Fee + $3.00 Exchange Profit)
        BigDecimal expectedPlatformFundChange = new BigDecimal("6.00");
        BigDecimal actualPlatformFundChange = platformFund.getBalance().subtract(initialPlatformFundBalance);
        assertThat(actualPlatformFundChange).isEqualByComparingTo(expectedPlatformFundChange);

        BigDecimal expectedBranchAFundChange = new BigDecimal("-1384.85");
        BigDecimal actualBranchAFundChange = branchAFund.getBalance().subtract(initialBranchAFundBalance);
        assertThat(actualBranchAFundChange).isEqualByComparingTo(expectedBranchAFundChange);

        BigDecimal expectedBranchBFundChange = new BigDecimal("1367.85");
        BigDecimal actualBranchBFundChange = branchBFund.getBalance().subtract(initialBranchBFundBalance);
        assertThat(actualBranchBFundChange).isEqualByComparingTo(expectedBranchBFundChange);
    }

    @Test
    public void testLargeTransaction_MultipleThousandUnits() {
        // Test large transaction: $5,000 USD (5 units of 1000)
        
        TransferTransactionRequest request = new TransferTransactionRequest();
        request.setSenderId(sender.getId());
        request.setReceiverId(receiver.getId());
        request.setFundId(branchAFund.getId());
        request.setAmount(new BigDecimal("5000.00"));
        request.setSourceCurrency("USD");
        request.setDestinationCurrency("EUR");
        request.setSenderBranchId(branchA.getId());
        request.setReceiverBranchId(branchB.getId());

        // Record initial balances
        BigDecimal initialSenderFundBalance = senderFund.getBalance();
        BigDecimal initialPlatformFundBalance = platformFund.getBalance();
        BigDecimal initialBranchAFundBalance = branchAFund.getBalance();
        BigDecimal initialBranchBFundBalance = branchBFund.getBalance();

        // Execute transaction
        TransactionRecordDTO result = transactionService.executeTransfer(request);

        // Verify transaction record
        assertThat(result.getGrossAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.getTotalFees()).isEqualByComparingTo("42.50"); // 5 * ($1.50 + $1.50 + $1.50 + $4.00)
        assertThat(result.getNetAmount()).isEqualByComparingTo("5000.00"); // clean principal
        assertThat(result.getPlatformExchangeProfit()).isEqualByComparingTo("7.50"); // 5 * $1.50 (USD ≠ EUR)

        // Refresh fund balances from database
        senderFund = fundRepository.findById(senderFund.getId()).orElseThrow();
        platformFund = fundRepository.findById(platformFund.getId()).orElseThrow();
        branchAFund = fundRepository.findById(branchAFund.getId()).orElseThrow();
        branchBFund = fundRepository.findById(branchBFund.getId()).orElseThrow();

        // Verify fund balance changes
        BigDecimal expectedSenderFundChange = BigDecimal.ZERO;
        BigDecimal actualSenderFundChange = senderFund.getBalance().subtract(initialSenderFundBalance);
        assertThat(actualSenderFundChange).isEqualByComparingTo(expectedSenderFundChange);

        // Platform Fund: +$15.00 (5 * $1.50 Platform Base Fee + 5 * $1.50 Exchange Profit)
        BigDecimal expectedPlatformFundChange = new BigDecimal("15.00");
        BigDecimal actualPlatformFundChange = platformFund.getBalance().subtract(initialPlatformFundBalance);
        assertThat(actualPlatformFundChange).isEqualByComparingTo(expectedPlatformFundChange);

        BigDecimal expectedBranchAFundChange = new BigDecimal("-5042.50");
        BigDecimal actualBranchAFundChange = branchAFund.getBalance().subtract(initialBranchAFundBalance);
        assertThat(actualBranchAFundChange).isEqualByComparingTo(expectedBranchAFundChange);

        BigDecimal expectedBranchBFundChange = new BigDecimal("5000.00");
        BigDecimal actualBranchBFundChange = branchBFund.getBalance().subtract(initialBranchBFundBalance);
        assertThat(actualBranchBFundChange).isEqualByComparingTo(expectedBranchBFundChange);
    }
}
