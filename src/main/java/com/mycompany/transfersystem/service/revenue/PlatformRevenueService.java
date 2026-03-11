package com.mycompany.transfersystem.service.revenue;

import com.mycompany.transfersystem.entity.PlatformRevenueEntry;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.PlatformRevenueEntryRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.accounting.AccountingLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PlatformRevenueService {

    private final PlatformRevenueEntryRepository revenueRepository;
    private final AccountingLedgerService accountingLedgerService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public PlatformRevenueService(PlatformRevenueEntryRepository revenueRepository,
                                  AccountingLedgerService accountingLedgerService,
                                  UserRepository userRepository,
                                  AuditService auditService) {
        this.revenueRepository = revenueRepository;
        this.accountingLedgerService = accountingLedgerService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void collect(String eventType, BigDecimal amount, String currency, Long sourceEntityId, String sourceEntityType) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        User systemUser = userRepository.findByUsername("SYSTEM").orElse(null);
        if (systemUser != null) {
            accountingLedgerService.postDoubleEntryForReference(
                    "PLATFORM_REVENUE", sourceEntityId != null ? sourceEntityId : 0L,
                    "REVENUE_SOURCE",
                    "PLATFORM_OWNER_REVENUE",
                    amount, currency != null ? currency : "USD",
                    "Platform revenue: " + eventType, systemUser);
        }
        PlatformRevenueEntry entry = PlatformRevenueEntry.builder()
                .eventType(eventType)
                .amount(amount)
                .currency(currency != null ? currency : "USD")
                .sourceEntityId(sourceEntityId)
                .sourceEntityType(sourceEntityType)
                .build();
        revenueRepository.save(entry);
        if (systemUser != null) {
            auditService.log("PLATFORM_REVENUE_COLLECTED", eventType, sourceEntityId != null ? sourceEntityId : 0L,
                    "amount=" + amount + " " + currency, systemUser);
        }
    }
}
