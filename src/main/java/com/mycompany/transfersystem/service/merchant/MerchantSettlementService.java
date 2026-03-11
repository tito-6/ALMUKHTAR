package com.mycompany.transfersystem.service.merchant;

import com.mycompany.transfersystem.entity.Merchant;
import com.mycompany.transfersystem.entity.MerchantSettlement;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.repository.MerchantRepository;
import com.mycompany.transfersystem.repository.MerchantSettlementRepository;
import com.mycompany.transfersystem.repository.MerchantTransactionRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class MerchantSettlementService {

    private static final Logger log = LoggerFactory.getLogger(MerchantSettlementService.class);

    private final MerchantRepository merchantRepository;
    private final MerchantTransactionRepository transactionRepository;
    private final MerchantSettlementRepository settlementRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final AuditService auditService;

    public MerchantSettlementService(MerchantRepository merchantRepository,
                                    MerchantTransactionRepository transactionRepository,
                                    MerchantSettlementRepository settlementRepository,
                                    WalletRepository walletRepository,
                                    WalletService walletService,
                                    AuditService auditService) {
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
        this.settlementRepository = settlementRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.auditService = auditService;
    }

    @Scheduled(cron = "0 0 23 * * *")
    @Transactional
    public void runDailySettlement() {
        LocalDate periodEnd = LocalDate.now().minusDays(1);
        LocalDate periodStart = periodEnd.minusDays(30);
        Instant since = periodStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant until = periodEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        for (Merchant merchant : merchantRepository.findByStatus("ACTIVE")) {
            try {
                BigDecimal net = transactionRepository.sumNetByMerchantBetween(merchant.getId(), since, until);
                if (net.compareTo(BigDecimal.ZERO) <= 0) continue;
                BigDecimal gross = net;
                BigDecimal fees = BigDecimal.ZERO;
                MerchantSettlement set = MerchantSettlement.builder()
                        .merchant(merchant)
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .grossAmount(gross)
                        .totalFees(fees)
                        .netAmount(net)
                        .status("PENDING")
                        .build();
                set = settlementRepository.save(set);

                Wallet merchantWallet = walletRepository.findByUser_Id(merchant.getOwnerUser().getId()).orElse(null);
                set.setSettlementWallet(merchantWallet);
                set.setStatus("SETTLED");
                set.setSettledAt(Instant.now());
                settlementRepository.save(set);
                auditService.log("MERCHANT_SETTLEMENT_PROCESSED", "MERCHANT_SETTLEMENT", set.getId(),
                        "net=" + net + " merchantId=" + merchant.getId(), merchant.getOwnerUser());
            } catch (Exception e) {
                log.warn("Settlement failed for merchant {}: {}", merchant.getId(), e.getMessage());
            }
        }
    }
}
