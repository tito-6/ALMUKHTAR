package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.*;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.AccountType;
import com.mycompany.transfersystem.entity.enums.EntryType;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.exception.InsufficientFundsException;
import com.mycompany.transfersystem.exception.InvalidTransactionException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.config.NotificationThresholdProperties;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.service.accounting.AccountingLedgerService;
import com.mycompany.transfersystem.service.liquidity.BranchCashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private FeeCalculationService feeCalculationService;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationThresholdProperties notificationThresholdProperties;

    @Autowired(required = false)
    private com.mycompany.transfersystem.service.accounting.AccountingLedgerService accountingLedgerService;

    @Autowired
    private BranchCashService branchCashService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return convertToResponse(transaction);
    }

    /**
     * Transaction record with passcode visibility derived from the authenticated actor's branch and role.
     */
    public TransactionRecordDTO getTransactionRecordForActor(Long id, User actor) {
        Objects.requireNonNull(actor, "actor");
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        Branch receiverBranch = transaction.getReceiver().getBranch() != null
                ? branchRepository.findById(transaction.getReceiver().getBranch().getId()).orElse(null)
                : null;

        TransactionRecordDTO record = new TransactionRecordDTO();
        record.setId(transaction.getId());
        record.setSenderId(transaction.getSender().getId());
        record.setReceiverId(transaction.getReceiver().getId());
        record.setFundId(transaction.getFund().getId());
        record.setGrossAmount(transaction.getAmount());
        record.setStatus(transaction.getStatus());
        record.setCreatedAt(transaction.getCreatedAt());
        record.setUpdatedAt(LocalDateTime.now());

        if (shouldHideReleasePasscode(receiverBranch, actor)) {
            record.setReleasePasscode(null);
        } else {
            record.setReleasePasscode(transaction.getReleasePasscode());
        }

        return record;
    }

    private boolean shouldHideReleasePasscode(Branch receiverBranch, User actor) {
        if (receiverBranch == null) {
            return false;
        }
        if (actor.getRole() == UserRole.AUDITOR) {
            return true;
        }
        if (actor.getRole() == UserRole.PLATFORM_OWNER
                || actor.getRole() == UserRole.SUPER_ADMIN
                || actor.getRole() == UserRole.MOTHER_BRANCH_ADMIN) {
            return false;
        }
        if (actor.getBranch() == null) {
            return false;
        }
        return actor.getBranch().getId().equals(receiverBranch.getId());
    }

    /**
     * @deprecated Prefer {@link #getTransactionRecordForActor(Long, User)} with a resolved {@link User}.
     */
    @Deprecated
    public TransactionRecordDTO getTransactionRecordById(Long id, Long requestingBranchId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        Branch receiverBranch = transaction.getReceiver().getBranch() != null
                ? branchRepository.findById(transaction.getReceiver().getBranch().getId()).orElse(null)
                : null;
        TransactionRecordDTO record = new TransactionRecordDTO();
        record.setId(transaction.getId());
        record.setSenderId(transaction.getSender().getId());
        record.setReceiverId(transaction.getReceiver().getId());
        record.setFundId(transaction.getFund().getId());
        record.setGrossAmount(transaction.getAmount());
        record.setStatus(transaction.getStatus());
        record.setCreatedAt(transaction.getCreatedAt());
        record.setUpdatedAt(LocalDateTime.now());
        if (receiverBranch != null && requestingBranchId != null && requestingBranchId.equals(receiverBranch.getId())) {
            record.setReleasePasscode(null);
        } else {
            record.setReleasePasscode(transaction.getReleasePasscode());
        }
        return record;
    }

    /**
     * Execute a comprehensive transfer transaction with fee calculation and fund routing
     */
    @Transactional
    public TransactionRecordDTO executeTransfer(TransferTransactionRequest request) {
        // Validate sender and receiver
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found with id: " + request.getSenderId()));
        
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + request.getReceiverId()));

        // Validate fund
        Fund fund = fundRepository.findByIdForUpdate(request.getFundId())
                .orElseThrow(() -> new ResourceNotFoundException("Fund not found with id: " + request.getFundId()));

        // Validate fund status
        if (fund.getStatus() != FundStatus.ACTIVE) {
            throw new InvalidTransactionException("Fund is not active");
        }

        // Validate sender and receiver are different
        if (sender.getId().equals(receiver.getId())) {
            throw new InvalidTransactionException("Sender and receiver cannot be the same");
        }

        // Get branches
        Branch senderBranch = branchRepository.findById(request.getSenderBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender branch not found with id: " + request.getSenderBranchId()));
        
        Branch receiverBranch = branchRepository.findById(request.getReceiverBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver branch not found with id: " + request.getReceiverBranchId()));

        // Get main admin branch for platform fees
        Branch mainAdminBranch = branchRepository.findFirstByName("MAIN_ADMIN_BRANCH")
                .orElseThrow(() -> new ResourceNotFoundException("Main admin branch not found"));

        // Create transaction fee request
        TransactionFeeRequest feeRequest = new TransactionFeeRequest();
        feeRequest.setAmount(request.getAmount());
        feeRequest.setSourceCurrency(request.getSourceCurrency());
        feeRequest.setDestinationCurrency(request.getDestinationCurrency());
        feeRequest.setSenderBranchId(request.getSenderBranchId());
        feeRequest.setReceiverBranchId(request.getReceiverBranchId());
        feeRequest.setSenderId(request.getSenderId());

        // Calculate fees using the new fee calculation service
        FeeBreakdownDTO feeBreakdown = feeCalculationService.calculateTransactionFees(feeRequest);

        // Get exchange rate
        BigDecimal exchangeRate = exchangeRateService.getRate(request.getSourceCurrency(), request.getDestinationCurrency());

        BigDecimal usdEquivalent = feeBreakdown.getUsdEquivalent().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFees = feeBreakdown.getTotalFee().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSettlementDebit = usdEquivalent.add(totalFees).setScale(2, RoundingMode.HALF_UP);

        // Validate sufficient balance in the source settlement fund. Comprehensive
        // transfer settlement is maintained in USD-equivalent branch accounting.
        if (fund.getBalance().compareTo(totalSettlementDebit) < 0) {
            throw new InsufficientFundsException("Insufficient balance in fund: " + fund.getName() + 
                    ". Required: " + totalSettlementDebit + ", Available: " + fund.getBalance());
        }

        // Get platform fund (main admin branch fund)
        Fund platformFund = getOrCreatePlatformFund(mainAdminBranch);

        // Get receiver branch fund
        Fund receiverBranchFund = getOrCreateBranchFund(receiverBranch);

        // Execute atomic transaction
        try {
            // 1. Debit the source branch settlement fund once for principal plus
            // every fee. This preserves the Sender Branch Pays All invariant.
            fund.setBalance(fund.getBalance().subtract(totalSettlementDebit));
            fundRepository.save(fund);

            // 2. Credit platform fund with platform fees
            BigDecimal platformFees = feeBreakdown.getPlatformBaseFee().add(feeBreakdown.getPlatformExchangeProfit());
            platformFund.setBalance(platformFund.getBalance().add(platformFees));
            fundRepository.save(platformFund);

            // 3. Credit receiver branch with the full principal. Receiving fee is
            // paid by the sender side and tracked as fee revenue in the ledger,
            // not deducted from the payout principal.
            receiverBranchFund.setBalance(receiverBranchFund.getBalance().add(usdEquivalent));
            fundRepository.save(receiverBranchFund);

            // 4. Define net principal for transaction record (USD equivalent)
            BigDecimal netPrincipal = usdEquivalent;

            // 5. Generate release passcode
            String releasePasscode = notificationService.generateReleasePasscode();

            // 6. Create transaction record
            Transaction transaction = new Transaction();
            transaction.setSender(sender);
            transaction.setReceiver(receiver);
            transaction.setFund(fund);
            transaction.setAmount(request.getAmount());
            transaction.setStatus(TransactionStatus.READY_FOR_PICKUP);
            transaction.setReleasePasscode(releasePasscode);
            transaction.setCurrencyCode(request.getDestinationCurrency());
            Transaction savedTransaction = transactionRepository.save(transaction);

            // 7. Reserve physical payout liquidity at the receiver branch before
            // the receiver is notified. This prevents overcommitting scarce cash.
            User currentUser = resolveActorOrSystem();
            branchCashService.reservePayout(savedTransaction, receiverBranch,
                    request.getDestinationCurrency(), netPrincipal, currentUser);

            boolean highValue = request.getAmount().compareTo(notificationThresholdProperties.getTransferHighValue()) >= 0;
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TransferCreatedEvent(
                    savedTransaction.getId(),
                    sender.getId(),
                    receiver.getId(),
                    savedTransaction.getAmount(),
                    request.getDestinationCurrency(),
                    totalFees,
                    request.getSenderBranchId(),
                    request.getReceiverBranchId(),
                    "READY_FOR_PICKUP",
                    false,
                    true,
                    highValue));
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TransferReadyForPickupEvent(
                    savedTransaction.getId(),
                    sender.getId(),
                    receiver.getId(),
                    savedTransaction.getAmount(),
                    request.getDestinationCurrency(),
                    totalFees,
                    request.getSenderBranchId(),
                    request.getReceiverBranchId(),
                    highValue));

            String branchAlert = "Transaction ID: " + savedTransaction.getId()
                    + " — Sender Branch: " + senderBranch.getName()
                    + " — Receiver: " + receiver.getUsername()
                    + " — Amount: " + request.getAmount()
                    + " — Currency: " + request.getDestinationCurrency()
                    + " — Status: READY_FOR_PICKUP";
            notificationService.sendInternalBranchAlert(receiverBranch.getId(), branchAlert);
            notificationService.sendEmail(sender, "Money transfer initiated — ready for receiver pickup",
                    "Transaction ID: " + savedTransaction.getId()
                            + ". Amount " + request.getAmount() + " " + request.getDestinationCurrency()
                            + ". Release Passcode: " + releasePasscode
                            + ". Receiver collects at branch " + receiverBranch.getName() + ".");
            notificationService.sendSMS(sender,
                    "ID: " + savedTransaction.getId() + " Passcode: " + releasePasscode);

            // 8. Log the transaction (if authentication is available)
            auditService.log("EXECUTE_TRANSFER", currentUser, "Transaction", savedTransaction.getId());
            if (accountingLedgerService != null) {
                accountingLedgerService.postBalancedEntries(savedTransaction, List.of(
                        ledgerLine("SRC_CASH_" + senderBranch.getId(), EntryType.DEBIT, totalSettlementDebit, AccountType.ASSET,
                                "Source branch pays principal and all fees"),
                        ledgerLine("DST_PAY_" + receiverBranch.getId(), EntryType.CREDIT, usdEquivalent, AccountType.LIABILITY,
                                "Destination branch clean principal payout"),
                        ledgerLine("PLAT_REV", EntryType.CREDIT, platformFees, AccountType.REVENUE,
                                "Platform base fee and exchange spread"),
                        ledgerLine("SND_FEE_" + senderBranch.getId(), EntryType.CREDIT, feeBreakdown.getSendingBranchFee(), AccountType.REVENUE,
                                "Sending branch fee revenue"),
                        ledgerLine("RCV_FEE_" + receiverBranch.getId(), EntryType.CREDIT, feeBreakdown.getReceivingBranchFee(), AccountType.REVENUE,
                                "Receiving branch fee revenue")
                ), "Sender Branch Pays All transfer " + savedTransaction.getId(), currentUser);
            }

            // 10. Create comprehensive transaction record
            return createTransactionRecord(savedTransaction, request, feeBreakdown, exchangeRate, 
                    senderBranch, receiverBranch, platformFees, netPrincipal, totalSettlementDebit);

        } catch (Exception e) {
            throw new InvalidTransactionException("Transaction failed: " + e.getMessage());
        }
    }

    @Transactional
    public TransactionResponse createTransfer(TransferRequest request) {
        // Validate sender and receiver
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found with id: " + request.getSenderId()));
        
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + request.getReceiverId()));

        // Validate fund
        Fund fund = fundRepository.findById(request.getFundId())
                .orElseThrow(() -> new ResourceNotFoundException("Fund not found with id: " + request.getFundId()));

        // Validate fund status
        if (fund.getStatus() != FundStatus.ACTIVE) {
            throw new InvalidTransactionException("Fund is not active");
        }

        // Validate sender and receiver are different
        if (sender.getId().equals(receiver.getId())) {
            throw new InvalidTransactionException("Sender and receiver cannot be the same");
        }

        // Validate sufficient balance
        if (fund.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance in fund: " + fund.getName());
        }

        // Create transaction with PENDING status
        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setFund(fund);
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.PENDING);

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            // Update fund balance
            fund.setBalance(fund.getBalance().subtract(request.getAmount()));
            fundRepository.save(fund);

            // Update transaction status to COMPLETED
            savedTransaction.setStatus(TransactionStatus.COMPLETED);
            savedTransaction = transactionRepository.save(savedTransaction);
            boolean highValue = request.getAmount().compareTo(notificationThresholdProperties.getTransferHighValue()) >= 0;
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TransferCreatedEvent(
                    savedTransaction.getId(),
                    sender.getId(),
                    receiver.getId(),
                    savedTransaction.getAmount(),
                    "USD",
                    BigDecimal.ZERO,
                    null,
                    null,
                    "COMPLETED",
                    true,
                    false,
                    highValue));
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TransferCompletedEvent(
                    savedTransaction.getId(),
                    sender.getId(),
                    receiver.getId(),
                    savedTransaction.getAmount(),
                    "USD",
                    BigDecimal.ZERO,
                    null,
                    null,
                    false,
                    highValue));

            // Log the transaction and post to ledger
            User currentUser = getCurrentUser();
            auditService.log("CREATE_TRANSACTION", currentUser, "Transaction", savedTransaction.getId());
            if (accountingLedgerService != null) {
                String debitCode = "RECEIVER-" + receiver.getId();
                String creditCode = "FUND-" + fund.getId();
                accountingLedgerService.postDoubleEntry(savedTransaction, debitCode, creditCode,
                        request.getAmount(), "USD", "Transfer " + savedTransaction.getId(), currentUser);
            }
        } catch (Exception e) {
            // If something goes wrong, mark transaction as FAILED
            savedTransaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(savedTransaction);
            applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TransferFailedEvent(
                    savedTransaction.getId(),
                    sender.getId(),
                    receiver.getId(),
                    "PROCESSING_ERROR"));
            throw new InvalidTransactionException("Transaction failed: " + e.getMessage());
        }

        return convertToResponse(savedTransaction);
    }

    /**
     * Create a PENDING transfer for QR release flow. Deducts from fund and generates passcode,
     * but leaves transaction PENDING until QR is scanned at receiving branch.
     */
    @Transactional
    public TransactionResponse createTransferForQr(TransferRequest request) {
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found with id: " + request.getSenderId()));

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + request.getReceiverId()));

        Fund fund = fundRepository.findById(request.getFundId())
                .orElseThrow(() -> new ResourceNotFoundException("Fund not found with id: " + request.getFundId()));

        if (fund.getStatus() != FundStatus.ACTIVE) {
            throw new InvalidTransactionException("Fund is not active");
        }

        if (sender.getId().equals(receiver.getId())) {
            throw new InvalidTransactionException("Sender and receiver cannot be the same");
        }

        if (fund.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance in fund: " + fund.getName());
        }

        String releasePasscode = notificationService.generateReleasePasscode();

        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setFund(fund);
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setReleasePasscode(releasePasscode);
        transaction.setCurrencyCode("USD");
        Transaction savedTransaction = transactionRepository.save(transaction);

        Branch payoutBranch = receiver.getBranch();
        if (payoutBranch != null) {
            branchCashService.reservePayout(savedTransaction, payoutBranch, "USD", request.getAmount(), resolveActorOrSystem());
        }

        fund.setBalance(fund.getBalance().subtract(request.getAmount()));
        fundRepository.save(fund);

        try {
            User currentUser = getCurrentUser();
            auditService.log("CREATE_TRANSFER_QR", currentUser, "Transaction", savedTransaction.getId());
        } catch (Exception e) {
            // Skip if no auth context
        }

        boolean highValue = request.getAmount().compareTo(notificationThresholdProperties.getTransferHighValue()) >= 0;
        Long senderBranchId = sender.getBranch() != null ? sender.getBranch().getId() : null;
        Long receiverBranchId = payoutBranch != null ? payoutBranch.getId() : null;
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.TransferCreatedEvent(
                savedTransaction.getId(),
                sender.getId(),
                receiver.getId(),
                request.getAmount(),
                "USD",
                BigDecimal.ZERO,
                senderBranchId,
                receiverBranchId,
                "PENDING",
                false,
                true,
                highValue));

        return convertToResponse(savedTransaction);
    }

    public List<TransactionResponse> getTransactionsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return transactionRepository.findByUser(user).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse convertToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSender().getUsername(),
                transaction.getReceiver().getUsername(),
                transaction.getFund().getName(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private User resolveActorOrSystem() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
                return userRepository.findByUsername(authentication.getName()).orElseGet(this::getSystemUser);
            }
        } catch (Exception ignored) {
            // Fall through to SYSTEM for tests, schedulers, and internal calls.
        }
        return getSystemUser();
    }

    private User getSystemUser() {
        return userRepository.findByUsername("SYSTEM")
                .orElseThrow(() -> new IllegalStateException("SYSTEM user must exist for audit logging"));
    }

    /**
     * Get or create platform fund for main admin branch
     */
    private Fund getOrCreatePlatformFund(Branch mainAdminBranch) {
        return fundRepository.findByNameForUpdate("Platform Fund")
                .orElseGet(() -> {
                    Fund platformFund = new Fund();
                    platformFund.setName("Platform Fund");
                    platformFund.setBalance(new BigDecimal("1000000.00")); // Starting balance
                    platformFund.setStatus(FundStatus.ACTIVE);
                    return fundRepository.save(platformFund);
                });
    }

    /**
     * Get or create branch fund for a specific branch
     */
    private Fund getOrCreateBranchFund(Branch branch) {
        String fundName = branch.getName() + " Fund";
        return fundRepository.findByNameForUpdate(fundName)
                .orElseGet(() -> {
                    Fund branchFund = new Fund();
                    branchFund.setName(fundName);
                    branchFund.setBalance(new BigDecimal("1000000.00")); // Starting balance
                    branchFund.setStatus(FundStatus.ACTIVE);
                    return fundRepository.save(branchFund);
                });
    }

    /**
     * Create comprehensive transaction record
     */
    private TransactionRecordDTO createTransactionRecord(Transaction transaction, 
                                                        TransferTransactionRequest request,
                                                        FeeBreakdownDTO feeBreakdown,
                                                        BigDecimal exchangeRate,
                                                        Branch senderBranch,
                                                        Branch receiverBranch,
                                                        BigDecimal platformFees,
                                                        BigDecimal netPrincipal,
                                                        BigDecimal totalSettlementDebit) {
        
        TransactionRecordDTO record = new TransactionRecordDTO();
        record.setId(transaction.getId());
        record.setSenderId(transaction.getSender().getId());
        record.setReceiverId(transaction.getReceiver().getId());
        record.setFundId(transaction.getFund().getId());
        
        // Transaction amounts
        record.setGrossAmount(request.getAmount());
        record.setNetAmount(netPrincipal);
        record.setTotalFees(feeBreakdown.getTotalFee());
        
        // Currency information
        record.setSourceCurrency(request.getSourceCurrency());
        record.setDestinationCurrency(request.getDestinationCurrency());
        record.setExchangeRate(exchangeRate);
        record.setUsdEquivalent(feeBreakdown.getUsdEquivalent());
        
        // Fee breakdown
        record.setPlatformBaseFee(feeBreakdown.getPlatformBaseFee());
        record.setPlatformExchangeProfit(feeBreakdown.getPlatformExchangeProfit());
        record.setSendingBranchFee(feeBreakdown.getSendingBranchFee());
        record.setReceivingBranchFee(feeBreakdown.getReceivingBranchFee());
        
        // Branch information
        record.setSenderBranchId(senderBranch.getId());
        record.setReceiverBranchId(receiverBranch.getId());
        record.setSenderBranchName(senderBranch.getName());
        record.setReceiverBranchName(receiverBranch.getName());
        
        // Transaction status and timing
        record.setStatus(transaction.getStatus());
        record.setCreatedAt(transaction.getCreatedAt());
        record.setUpdatedAt(LocalDateTime.now());
        
        // Fund routing information
        record.setPlatformFundCredit(platformFees);
        record.setSenderBranchFundCredit(totalSettlementDebit.negate()); // Negative because sender branch pays all costs
        record.setReceiverBranchFundCredit(feeBreakdown.getUsdEquivalent()); // Full clean principal
        record.setInterBranchDebt(feeBreakdown.getUsdEquivalent()); // USD equivalent transferred between branches
        
        // Security information
        record.setReleasePasscode(transaction.getReleasePasscode());
        
        return record;
    }

    private AccountingLedgerService.LedgerLine ledgerLine(String code,
                                                          EntryType entryType,
                                                          BigDecimal amount,
                                                          AccountType accountType,
                                                          String description) {
        return new AccountingLedgerService.LedgerLine(
                truncateAccountCode(code),
                entryType,
                amount.setScale(2, RoundingMode.HALF_UP),
                "USD",
                accountType,
                description);
    }

    private String truncateAccountCode(String code) {
        return code.length() <= 20 ? code : code.substring(0, 20);
    }
}
