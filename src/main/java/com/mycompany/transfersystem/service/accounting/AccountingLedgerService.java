package com.mycompany.transfersystem.service.accounting;

import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.AccountType;
import com.mycompany.transfersystem.entity.enums.EntryType;
import com.mycompany.transfersystem.exception.AccountingImbalanceException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.ChartOfAccountRepository;
import com.mycompany.transfersystem.repository.LedgerEntryRepository;
import com.mycompany.transfersystem.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountingLedgerService {

    private static final Logger log = LoggerFactory.getLogger(AccountingLedgerService.class);

    private final LedgerEntryRepository ledgerRepository;
    private final ChartOfAccountRepository accountRepository;
    private final AuditService auditService;

    public AccountingLedgerService(LedgerEntryRepository ledgerRepository,
                                   ChartOfAccountRepository accountRepository,
                                   AuditService auditService) {
        this.ledgerRepository = ledgerRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void postDoubleEntry(Transaction transaction,
                                String debitAccountCode,
                                String creditAccountCode,
                                BigDecimal amount,
                                String currency,
                                String description,
                                User actor) {
        ChartOfAccount debitAccount = findOrCreateAccount(debitAccountCode);
        ChartOfAccount creditAccount = findOrCreateAccount(creditAccountCode);

        LedgerEntry debit = LedgerEntry.builder()
                .transaction(transaction)
                .account(debitAccount)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .currencyCode(currency != null ? currency : "USD")
                .description(description)
                .createdBy(actor)
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .transaction(transaction)
                .account(creditAccount)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .currencyCode(currency != null ? currency : "USD")
                .description(description)
                .createdBy(actor)
                .build();

        ledgerRepository.save(debit);
        ledgerRepository.save(credit);

        assertLedgerBalance(transaction.getId());

        auditService.log("LEDGER_POSTED", "LedgerEntry", transaction.getId(),
                String.format("D:%s C:%s AMT:%.4f %s", debitAccountCode, creditAccountCode, amount, currency),
                actor);
    }

    /**
     * Post double-entry for wallet or other non-Transaction references.
     */
    @Transactional
    public void postDoubleEntryForReference(String referenceType,
                                            Long referenceId,
                                            String debitAccountCode,
                                            String creditAccountCode,
                                            BigDecimal amount,
                                            String currency,
                                            String description,
                                            User actor) {
        ChartOfAccount debitAccount = findOrCreateAccount(debitAccountCode);
        ChartOfAccount creditAccount = findOrCreateAccount(creditAccountCode);

        LedgerEntry debit = LedgerEntry.builder()
                .transaction(null)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .account(debitAccount)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .currencyCode(currency != null ? currency : "USD")
                .description(description)
                .createdBy(actor)
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .transaction(null)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .account(creditAccount)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .currencyCode(currency != null ? currency : "USD")
                .description(description)
                .createdBy(actor)
                .build();

        ledgerRepository.save(debit);
        ledgerRepository.save(credit);

        auditService.log("LEDGER_POSTED", referenceType, referenceId != null ? referenceId : 0L,
                String.format("D:%s C:%s AMT:%.4f %s", debitAccountCode, creditAccountCode, amount, currency),
                actor);
    }

    @Transactional
    public void postBalancedEntries(Transaction transaction,
                                    List<LedgerLine> lines,
                                    String description,
                                    User actor) {
        if (lines == null || lines.isEmpty()) {
            throw new AccountingImbalanceException("No ledger lines supplied");
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerLine line : lines) {
            if (line.amount() == null || line.amount().compareTo(BigDecimal.ZERO) < 0) {
                throw new AccountingImbalanceException("Ledger line amount must be positive");
            }
            if (line.amount().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (line.entryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(line.amount());
            } else {
                totalCredits = totalCredits.add(line.amount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new AccountingImbalanceException(
                    "Ledger imbalance before posting: debits=" + totalDebits + " credits=" + totalCredits);
        }

        for (LedgerLine line : lines) {
            if (line.amount().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            ChartOfAccount account = findOrCreateAccount(line.accountCode(), line.accountType());
            ledgerRepository.save(LedgerEntry.builder()
                    .transaction(transaction)
                    .account(account)
                    .entryType(line.entryType())
                    .amount(line.amount())
                    .currencyCode(line.currency() != null ? line.currency() : "USD")
                    .description(line.description() != null ? line.description() : description)
                    .createdBy(actor)
                    .build());
        }

        assertLedgerBalance(transaction.getId());

        auditService.log("LEDGER_POSTED", "LedgerEntry", transaction.getId(),
                description != null ? description : "Balanced ledger entries", actor);
    }

    public void assertLedgerBalance(Long transactionId) {
        BigDecimal totalDebits = ledgerRepository.sumByTransactionAndType(transactionId, EntryType.DEBIT);
        BigDecimal totalCredits = ledgerRepository.sumByTransactionAndType(transactionId, EntryType.CREDIT);
        if (totalDebits == null) totalDebits = BigDecimal.ZERO;
        if (totalCredits == null) totalCredits = BigDecimal.ZERO;
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new AccountingImbalanceException(
                    "Ledger imbalance for transaction " + transactionId +
                            ": debits=" + totalDebits + " credits=" + totalCredits);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(String accountCode) {
        ChartOfAccount account = accountRepository.findByCode(accountCode)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountCode));
        BigDecimal balance = ledgerRepository.calculateBalance(account.getId());
        return balance != null ? balance : BigDecimal.ZERO;
    }

    private ChartOfAccount findOrCreateAccount(String code) {
        return findOrCreateAccount(code, AccountType.ASSET);
    }

    private ChartOfAccount findOrCreateAccount(String code, AccountType accountType) {
        return accountRepository.findByCode(code).orElseGet(() -> {
            log.warn("Auto-creating ledger account for code: {}", code);
            return accountRepository.save(ChartOfAccount.builder()
                    .code(code)
                    .name("Auto: " + code)
                    .accountType(accountType != null ? accountType : AccountType.ASSET)
                    .system(true)
                    .build());
        });
    }

    public record LedgerLine(String accountCode,
                             EntryType entryType,
                             BigDecimal amount,
                             String currency,
                             AccountType accountType,
                             String description) {
    }
}
