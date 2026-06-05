package com.mycompany.transfersystem.service.recurring;

import com.mycompany.transfersystem.dto.recurring.CreateRecurringTransferRequest;
import com.mycompany.transfersystem.entity.RecurringTransfer;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.exception.ConditionNotMetException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.RecurringTransferRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecurringTransferService {

    private final RecurringTransferRepository recurringTransferRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final AuditService auditService;

    @Transactional
    public RecurringTransfer create(Long userId, CreateRecurringTransferRequest dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        RecurringTransfer.Frequency freq = RecurringTransfer.Frequency.valueOf(dto.getFrequency().toUpperCase());
        RecurringTransfer rt = RecurringTransfer.builder()
                .ownerUser(user)
                .destinationUserId(dto.getDestinationUserId())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .frequency(freq)
                .nextRunAt(dto.getFirstRunAt() != null ? dto.getFirstRunAt() : LocalDateTime.now().plusDays(1))
                .build();
        rt = recurringTransferRepository.save(rt);
        auditService.log("RECURRING_TRANSFER_CREATED", "RECURRING_TRANSFER", rt.getId(), null, user);
        return rt;
    }

    @Transactional
    public void pause(Long id, Long userId) {
        RecurringTransfer rt = getOwnedTransfer(id, userId);
        rt.setStatus(RecurringTransfer.RecurringStatus.PAUSED);
        recurringTransferRepository.save(rt);
        auditService.log("RECURRING_TRANSFER_PAUSED", "RECURRING_TRANSFER", id, null, rt.getOwnerUser());
    }

    @Transactional
    public void resume(Long id, Long userId) {
        RecurringTransfer rt = getOwnedTransfer(id, userId);
        rt.setStatus(RecurringTransfer.RecurringStatus.ACTIVE);
        recurringTransferRepository.save(rt);
        auditService.log("RECURRING_TRANSFER_RESUMED", "RECURRING_TRANSFER", id, null, rt.getOwnerUser());
    }

    @Transactional
    public void cancel(Long id, Long userId) {
        RecurringTransfer rt = getOwnedTransfer(id, userId);
        rt.setStatus(RecurringTransfer.RecurringStatus.CANCELLED);
        recurringTransferRepository.save(rt);
        auditService.log("RECURRING_TRANSFER_CANCELLED", "RECURRING_TRANSFER", id, null, rt.getOwnerUser());
    }

    @Scheduled(cron = "0 */15 * * * *")
    @SchedulerLock(name = "recurring-transfers", lockAtMostFor = "PT10M")
    @Transactional
    public void processDue() {
        try {
            List<RecurringTransfer> due = recurringTransferRepository.findActiveByNextRunAtBefore(LocalDateTime.now());
            for (RecurringTransfer rt : due) {
                try {
                    Wallet senderWallet = walletRepository.findByUser_Id(rt.getOwnerUser().getId()).orElse(null);
                    Wallet receiverWallet = walletRepository.findByUser_Id(rt.getDestinationUserId()).orElse(null);
                    if (senderWallet == null || receiverWallet == null) {
                        log.warn("Wallet not found for recurring transfer {}", rt.getId());
                        advanceNextRun(rt);
                        continue;
                    }
                    walletService.debit(senderWallet.getId(), rt.getCurrency(), rt.getAmount(),
                            WalletTransactionType.TRANSFER, "REC-" + rt.getId(), "Recurring transfer");
                    walletService.credit(receiverWallet.getId(), rt.getCurrency(), rt.getAmount(),
                            WalletTransactionType.TRANSFER, "REC-" + rt.getId(), "Recurring transfer received");
                    advanceNextRun(rt);
                    log.info("Processed recurring transfer {}", rt.getId());
                } catch (Exception e) {
                    log.warn("Insufficient funds or error for recurring transfer {}: {}", rt.getId(), e.getMessage());
                    advanceNextRun(rt);
                }
            }
        } catch (Exception e) {
            log.error("Error processing recurring transfers", e);
        }
    }

    private void advanceNextRun(RecurringTransfer rt) {
        LocalDateTime next = switch (rt.getFrequency()) {
            case DAILY -> rt.getNextRunAt().plusDays(1);
            case WEEKLY -> rt.getNextRunAt().plusWeeks(1);
            case MONTHLY -> rt.getNextRunAt().plusMonths(1);
        };
        rt.setNextRunAt(next);
        recurringTransferRepository.save(rt);
    }

    private RecurringTransfer getOwnedTransfer(Long id, Long userId) {
        RecurringTransfer rt = recurringTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transfer not found: " + id));
        if (!rt.getOwnerUser().getId().equals(userId)) {
            throw new ConditionNotMetException("Not your recurring transfer");
        }
        return rt;
    }

    @Transactional(readOnly = true)
    public List<RecurringTransfer> getMyTransfers(Long userId) {
        return recurringTransferRepository.findByOwnerUserIdAndStatusNot(userId, RecurringTransfer.RecurringStatus.CANCELLED);
    }
}
