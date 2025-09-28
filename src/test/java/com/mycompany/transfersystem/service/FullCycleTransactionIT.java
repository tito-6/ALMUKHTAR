package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.*;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FullCycleTransactionIT {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ReleasePasscodeService releasePasscodeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private NotificationService notificationService;

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

        // Create test users with proper branch associations
        sender = new User();
        sender.setUsername("testSender");
        sender.setPassword(passwordEncoder.encode("password123"));
        sender.setRole(UserRole.CASHIER);
        sender.setEmail("sender@test.com");
        sender.setPhone("+1234567890");
        sender.setBranch(branchA); // Sender belongs to Branch A
        sender = userRepository.save(sender);

        receiver = new User();
        receiver.setUsername("testReceiver");
        receiver.setPassword(passwordEncoder.encode("password123"));
        receiver.setRole(UserRole.CASHIER);
        receiver.setEmail("receiver@test.com");
        receiver.setPhone("+0987654321");
        receiver.setBranch(branchB); // Receiver belongs to Branch B
        receiver = userRepository.save(receiver);

        // Create sender fund with $5,000 starting balance
        senderFund = new Fund();
        senderFund.setName("Sender Test Fund");
        senderFund.setBalance(new BigDecimal("5000.00"));
        senderFund.setStatus(FundStatus.ACTIVE);
        senderFund = fundRepository.save(senderFund);

        // Create platform fund with $1,000,000 starting balance
        platformFund = new Fund();
        platformFund.setName("Platform Fund");
        platformFund.setBalance(new BigDecimal("1000000.00"));
        platformFund.setStatus(FundStatus.ACTIVE);
        platformFund = fundRepository.save(platformFund);

        // Create branch A fund with $5,000 starting balance
        branchAFund = new Fund();
        branchAFund.setName("BRANCH_A Fund");
        branchAFund.setBalance(new BigDecimal("5000.00"));
        branchAFund.setStatus(FundStatus.ACTIVE);
        branchAFund = fundRepository.save(branchAFund);

        // Create branch B fund with $5,000 starting balance
        branchBFund = new Fund();
        branchBFund.setName("BRANCH_B Fund");
        branchBFund.setBalance(new BigDecimal("5000.00"));
        branchBFund.setStatus(FundStatus.ACTIVE);
        branchBFund = fundRepository.save(branchBFund);

        // Reset mock interactions
        Mockito.reset(notificationService);
        
        // Configure mock to return a test passcode
        when(notificationService.generateReleasePasscode()).thenReturn("123456");
        
        // Configure mock to allow multiple calls to sendEmail
        doNothing().when(notificationService).sendEmail(any(), any(), any());
    }

    @Test
    public void testFullCycleTransaction_USDToEUR_WithSecurityAndNotifications() {
        // Test Scenario: Branch A sends 1,000 USD to be delivered as EURO at Branch B
        
        // Step 1: Preparation & Execution
        TransferTransactionRequest request = new TransferTransactionRequest();
        request.setSenderId(sender.getId());
        request.setReceiverId(receiver.getId());
        request.setFundId(senderFund.getId());
        request.setAmount(new BigDecimal("1000.00"));
        request.setSourceCurrency("USD");
        request.setDestinationCurrency("EUR");
        request.setSenderBranchId(branchA.getId());
        request.setReceiverBranchId(branchB.getId());

        // Record initial balances
        BigDecimal initialSenderFundBalance = senderFund.getBalance();
        BigDecimal initialBranchAFundBalance = branchAFund.getBalance();
        BigDecimal initialBranchBFundBalance = branchBFund.getBalance();

        // Execute the POST /api/transactions call for the 1,000 USD to EUR transfer
        TransactionRecordDTO result = transactionService.executeTransfer(request);

        // Step 2: Verify Transaction Status and Basic Information
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getGrossAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.getSourceCurrency()).isEqualTo("USD");
        assertThat(result.getDestinationCurrency()).isEqualTo("EUR");
        assertThat(result.getSenderBranchId()).isEqualTo(branchA.getId());
        assertThat(result.getReceiverBranchId()).isEqualTo(branchB.getId());
        assertThat(result.getReleasePasscode()).isNotNull();
        assertThat(result.getReleasePasscode()).hasSize(6); // 6-digit passcode

        // Step 3: Verify Communication and Passcode Security
        // Verify NotificationService.sendInternalBranchAlert was called once for Branch B
        verify(notificationService, times(1)).sendInternalBranchAlert(
            eq(branchB.getId()),
            argThat(message -> 
                message.contains("Transaction ID: " + result.getId()) &&
                message.contains("Sender Branch: " + branchA.getName()) &&
                message.contains("Receiver: " + receiver.getUsername()) &&
                message.contains("Amount: 1000.00") &&
                !message.contains(result.getReleasePasscode()) // NO PASSCODE in branch alert
            )
        );

        // Verify NotificationService.sendEmail was called once for the Sender Client
        verify(notificationService, times(1)).sendEmail(
            eq(sender),
            eq("Money Transfer Processed"),
            argThat(message -> 
                message.contains("Transaction ID: " + result.getId()) &&
                message.contains("Release Passcode: " + result.getReleasePasscode())
            )
        );

        // Verify NotificationService.sendSMS was called once for the Sender Client
        verify(notificationService, times(1)).sendSMS(
            eq(sender),
            argThat(message -> 
                message.contains("ID: " + result.getId()) &&
                message.contains("Passcode: " + result.getReleasePasscode())
            )
        );

        // Step 4: Verify Passcode Security - Branch B cannot see the passcode
        TransactionRecordDTO branchBView = transactionService.getTransactionRecordById(result.getId(), branchB.getId());
        assertThat(branchBView.getReleasePasscode()).isNull(); // Branch B cannot see passcode

        // Step 5: Verify Fund Accounting (Inter-Branch Debt)
        // Refresh fund balances from database
        senderFund = fundRepository.findById(senderFund.getId()).orElseThrow();
        branchAFund = fundRepository.findById(branchAFund.getId()).orElseThrow();
        branchBFund = fundRepository.findById(branchBFund.getId()).orElseThrow();

        // Calculate expected changes based on fee structure
        // Total fees: $1.50 (platform base) + $1.50 (exchange profit) + $1.50 (sending) + $4.00 (receiving) = $8.50
        BigDecimal totalFees = new BigDecimal("8.50");
        BigDecimal netPrincipal = new BigDecimal("1000.00").subtract(totalFees); // $991.50
        
        // Verify the calculated net principal matches the transaction record
        assertThat(result.getNetAmount()).isEqualByComparingTo(netPrincipal);

        // Verify Branch A Fund Balance is correctly debited
        BigDecimal expectedBranchAChange = new BigDecimal("-990.00"); // -$991.50 (net principal) + $1.50 (sending fee) = -$990.00
        BigDecimal actualBranchAChange = branchAFund.getBalance().subtract(initialBranchAFundBalance);
        assertThat(actualBranchAChange).isEqualByComparingTo(expectedBranchAChange);

        // Verify Branch B Fund Balance is correctly credited
        BigDecimal expectedBranchBChange = new BigDecimal("995.50"); // +$4.00 (receiving fee) + $991.50 (net principal)
        BigDecimal actualBranchBChange = branchBFund.getBalance().subtract(initialBranchBFundBalance);
        assertThat(actualBranchBChange).isEqualByComparingTo(expectedBranchBChange);

        // Verify Sender Fund Balance is correctly debited
        BigDecimal expectedSenderFundChange = new BigDecimal("-1008.50"); // -$1000.00 (gross) - $8.50 (total fees)
        BigDecimal actualSenderFundChange = senderFund.getBalance().subtract(initialSenderFundBalance);
        assertThat(actualSenderFundChange).isEqualByComparingTo(expectedSenderFundChange);

        // Step 6: Execute Release & Final Confirmation
        ReleaseRequest releaseRequest = new ReleaseRequest();
        releaseRequest.setPasscode(result.getReleasePasscode());
        releaseRequest.setReceiverId(receiver.getId());

        // Call POST /api/transactions/{transactionId}/release using the correct passcode
        boolean released = releasePasscodeService.verifyPasscode(result.getId(), releaseRequest.getPasscode(), releaseRequest.getReceiverId());

        // Verify the transaction status is updated to RELEASED
        assertThat(released).isTrue();
        
        // Verify the transaction status in database
        TransactionRecordDTO releasedTransaction = transactionService.getTransactionRecordById(result.getId(), null);
        assertThat(releasedTransaction.getStatus()).isEqualTo(TransactionStatus.RELEASED);

        // Verify NotificationService.sendEmail was called for the Sender Client with "Released" confirmation
        verify(notificationService, times(1)).sendEmail(
            eq(sender),
            eq("Money Transfer Released"),
            argThat(message -> 
                message.contains("Transaction ID: " + result.getId()) &&
                message.contains("successfully released")
            )
        );

        // Step 7: Report Final Results
        System.out.println("=== FULL CYCLE TRANSACTION TEST RESULTS ===");
        System.out.println("Transaction ID: " + result.getId());
        System.out.println("Status: " + releasedTransaction.getStatus());
        System.out.println("Release Passcode: " + result.getReleasePasscode());
        System.out.println("Final Branch A Fund Balance: " + branchAFund.getBalance());
        System.out.println("Final Branch B Fund Balance: " + branchBFund.getBalance());
        System.out.println("Final Sender Fund Balance: " + senderFund.getBalance());
        System.out.println("Security Verification: PASSED - Branch B cannot see passcode");
        System.out.println("Notification Verification: PASSED - All notifications sent correctly");
        System.out.println("Fund Accounting Verification: PASSED - All balances updated correctly");
        System.out.println("Release Verification: PASSED - Transaction successfully released");
    }

    @Test
    public void testInvalidPasscodeRelease() {
        // Test invalid passcode scenario
        TransferTransactionRequest request = new TransferTransactionRequest();
        request.setSenderId(sender.getId());
        request.setReceiverId(receiver.getId());
        request.setFundId(senderFund.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setSourceCurrency("USD");
        request.setDestinationCurrency("USD");
        request.setSenderBranchId(branchA.getId());
        request.setReceiverBranchId(branchB.getId());

        TransactionRecordDTO result = transactionService.executeTransfer(request);

        // Try to release with wrong passcode
        ReleaseRequest releaseRequest = new ReleaseRequest();
        releaseRequest.setPasscode("000000"); // Wrong passcode
        releaseRequest.setReceiverId(receiver.getId());

        try {
            releasePasscodeService.verifyPasscode(result.getId(), releaseRequest.getPasscode(), releaseRequest.getReceiverId());
            assertThat(false).as("Should have thrown exception for invalid passcode").isTrue();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Invalid release passcode");
        }

        // Verify transaction status remains COMPLETED (not RELEASED)
        TransactionRecordDTO transaction = transactionService.getTransactionRecordById(result.getId(), null);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    public void testTLToUSDTransfer_BranchADebitAll() {
        // Test Scenario: Ahmad deposits 3,500 TL at Branch A, which debits its own fund for 
        // the USD equivalent and ALL associated fees. Muhammed receives USD at Branch B.
        
        // Step 1: Setup test data with correct initial fund balances
        // Branch A Fund: 8,000 USD (Initial)
        branchAFund.setBalance(new BigDecimal("8000.00"));
        branchAFund = fundRepository.save(branchAFund);
        
        // Branch B Fund: 3,000 USD (Initial)  
        branchBFund.setBalance(new BigDecimal("3000.00"));
        branchBFund = fundRepository.save(branchBFund);
        
        // Platform Fund: 1,000,000 USD (Initial)
        platformFund.setBalance(new BigDecimal("1000000.00"));
        platformFund = fundRepository.save(platformFund);
        
        // Record initial balances for verification
        BigDecimal initialBranchAFundBalance = branchAFund.getBalance();
        BigDecimal initialBranchBFundBalance = branchBFund.getBalance();
        BigDecimal initialPlatformFundBalance = platformFund.getBalance();
        
        // Step 2: Create TL to USD transfer request
        TransferTransactionRequest request = new TransferTransactionRequest();
        request.setSenderId(sender.getId());
        request.setReceiverId(receiver.getId());
        request.setFundId(senderFund.getId());
        request.setAmount(new BigDecimal("3500.00")); // 3,500 TL
        request.setSourceCurrency("TL");
        request.setDestinationCurrency("USD");
        request.setSenderBranchId(branchA.getId());
        request.setReceiverBranchId(branchB.getId());
        
        // Step 3: Execute the transaction
        TransactionRecordDTO result = transactionService.executeTransfer(request);
        
        // Step 4: Verify transaction execution
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getGrossAmount()).isEqualByComparingTo("3500.00");
        assertThat(result.getSourceCurrency()).isEqualTo("TL");
        assertThat(result.getDestinationCurrency()).isEqualTo("USD");
        assertThat(result.getSenderBranchId()).isEqualTo(branchA.getId());
        assertThat(result.getReceiverBranchId()).isEqualTo(branchB.getId());
        assertThat(result.getReleasePasscode()).isNotNull();
        assertThat(result.getReleasePasscode()).hasSize(6);
        
        // Step 5: Calculate expected fund changes
        // Get USD/TL exchange rate (assuming 1 USD = 30 TL for this test)
        BigDecimal exchangeRate = new BigDecimal("30.00"); // 1 USD = 30 TL
        BigDecimal usdEquivalent = new BigDecimal("3500.00").divide(exchangeRate, 2, BigDecimal.ROUND_HALF_UP); // 116.67 USD
        
        // Calculate fees based on USD equivalent
        BigDecimal sendingFee = usdEquivalent.multiply(new BigDecimal("0.0015")); // 1.50 per 1000
        BigDecimal platformFee = usdEquivalent.multiply(new BigDecimal("0.0015")); // 1.50 per 1000  
        BigDecimal receivingFee = usdEquivalent.multiply(new BigDecimal("0.004")); // 4.00 per 1000
        
        // Branch A should be debited for: USD equivalent + ALL fees
        BigDecimal totalBranchADebit = usdEquivalent.add(sendingFee).add(platformFee).add(receivingFee);
        
        // Branch B should be credited with: USD equivalent (receiving fee is covered by Branch A)
        BigDecimal branchBCredit = usdEquivalent;
        
        // Platform fund should be credited with: Platform fee
        BigDecimal platformCredit = platformFee;
        
        // Step 6: Refresh fund balances from database
        branchAFund = fundRepository.findById(branchAFund.getId()).orElseThrow();
        branchBFund = fundRepository.findById(branchBFund.getId()).orElseThrow();
        platformFund = fundRepository.findById(platformFund.getId()).orElseThrow();
        
        // Step 7: Verify Branch A Fund Integrity (Debit)
        BigDecimal actualBranchAChange = branchAFund.getBalance().subtract(initialBranchAFundBalance);
        BigDecimal expectedBranchAChange = totalBranchADebit.negate(); // Should be negative (debit)
        assertThat(actualBranchAChange).isEqualByComparingTo(expectedBranchAChange);
        
        // Step 8: Verify Branch B Fund Integrity (Credit)
        BigDecimal actualBranchBChange = branchBFund.getBalance().subtract(initialBranchBFundBalance);
        BigDecimal expectedBranchBChange = branchBCredit; // Should be positive (credit)
        assertThat(actualBranchBChange).isEqualByComparingTo(expectedBranchBChange);
        
        // Step 9: Verify Platform Fund Integrity (Credit)
        BigDecimal actualPlatformChange = platformFund.getBalance().subtract(initialPlatformFundBalance);
        BigDecimal expectedPlatformChange = platformCredit; // Should be positive (credit)
        assertThat(actualPlatformChange).isEqualByComparingTo(expectedPlatformChange);
        
        // Step 10: Release the transaction
        ReleaseRequest releaseRequest = new ReleaseRequest();
        releaseRequest.setPasscode(result.getReleasePasscode());
        releaseRequest.setReceiverId(receiver.getId());
        
        boolean released = releasePasscodeService.verifyPasscode(
            result.getId(), 
            releaseRequest.getPasscode(), 
            releaseRequest.getReceiverId()
        );
        
        // Step 11: Verify final transaction status
        assertThat(released).isTrue();
        TransactionRecordDTO releasedTransaction = transactionService.getTransactionRecordById(result.getId(), null);
        assertThat(releasedTransaction.getStatus()).isEqualTo(TransactionStatus.RELEASED);
        
        // Step 12: Report Final Results
        System.out.println("=== TL TO USD TRANSFER TEST RESULTS ===");
        System.out.println("Transaction ID: " + result.getId());
        System.out.println("Status: " + releasedTransaction.getStatus());
        System.out.println("Release Passcode: " + result.getReleasePasscode());
        System.out.println("USD Equivalent: " + usdEquivalent + " USD");
        System.out.println("Total Fees: " + sendingFee.add(platformFee).add(receivingFee) + " USD");
        System.out.println("Final Branch A Fund Balance: " + branchAFund.getBalance());
        System.out.println("Final Branch B Fund Balance: " + branchBFund.getBalance());
        System.out.println("Final Platform Fund Balance: " + platformFund.getBalance());
        System.out.println("Branch A Debit: " + actualBranchAChange + " USD");
        System.out.println("Branch B Credit: " + actualBranchBChange + " USD");
        System.out.println("Platform Credit: " + actualPlatformChange + " USD");
        System.out.println("Fund Accounting Verification: PASSED - Branch A debited all fees");
    }
}
