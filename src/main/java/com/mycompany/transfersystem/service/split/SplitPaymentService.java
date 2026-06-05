package com.mycompany.transfersystem.service.split;

import com.mycompany.transfersystem.dto.split.CreateSplitRequest;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.exception.ConditionNotMetException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.SplitParticipantRepository;
import com.mycompany.transfersystem.repository.SplitRequestRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SplitPaymentService {

    private final SplitRequestRepository splitRequestRepository;
    private final SplitParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final AuditService auditService;

    @Transactional
    public SplitRequest createSplit(Long initiatorId, CreateSplitRequest dto) {
        User initiator = userRepository.findById(initiatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + initiatorId));
        SplitRequest sr = SplitRequest.builder()
                .initiatorUser(initiator)
                .title(dto.getTitle())
                .totalAmount(dto.getTotalAmount())
                .currency(dto.getCurrency())
                .expiresAt(dto.getExpiresAt() != null ? dto.getExpiresAt() : LocalDateTime.now().plusDays(7))
                .build();
        sr = splitRequestRepository.save(sr);

        int count = dto.getParticipantUserIds().size();
        BigDecimal share = dto.getTotalAmount().divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
        for (Long uid : dto.getParticipantUserIds()) {
            User payee = userRepository.findById(uid)
                    .orElseThrow(() -> new ResourceNotFoundException("Participant user not found: " + uid));
            participantRepository.save(SplitParticipant.builder()
                    .splitRequest(sr).payeeUser(payee).shareAmount(share).build());
        }
        auditService.log("SPLIT_CREATED", "SPLIT_REQUEST", sr.getId(), "participants=" + count, initiator);
        return sr;
    }

    @Transactional
    public void respondToSplit(Long participantId, Long userId, boolean accept) {
        SplitParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Split participant not found: " + participantId));
        if (!participant.getPayeeUser().getId().equals(userId)) {
            throw new ConditionNotMetException("Not your split participation");
        }
        if (participant.isPaid()) {
            throw new ConditionNotMetException("Already responded");
        }
        SplitRequest sr = participant.getSplitRequest();
        if (!accept) {
            participant.setPaid(false);
            participantRepository.save(participant);
            auditService.log("SPLIT_DECLINED", "SPLIT_PARTICIPANT", participantId, null, participant.getPayeeUser());
            return;
        }
        Wallet payerWallet = walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user " + userId));
        Wallet initiatorWallet = walletRepository.findByUser_Id(sr.getInitiatorUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Initiator wallet not found"));
        walletService.debit(payerWallet.getId(), sr.getCurrency(), participant.getShareAmount(),
                WalletTransactionType.TRANSFER, "SPLIT-" + sr.getId(), "Split payment: " + sr.getTitle());
        walletService.credit(initiatorWallet.getId(), sr.getCurrency(), participant.getShareAmount(),
                WalletTransactionType.TRANSFER, "SPLIT-" + sr.getId(), "Split received: " + sr.getTitle());
        participant.setPaid(true);
        participant.setPaidAt(Instant.now());
        participantRepository.save(participant);
        List<SplitParticipant> all = participantRepository.findBySplitRequestId(sr.getId());
        boolean allPaid = all.stream().allMatch(SplitParticipant::isPaid);
        if (allPaid) {
            sr.setStatus(SplitRequest.SplitStatus.COMPLETED);
        } else {
            sr.setStatus(SplitRequest.SplitStatus.PARTIALLY_PAID);
        }
        splitRequestRepository.save(sr);
        auditService.log("SPLIT_PAID", "SPLIT_PARTICIPANT", participantId, "amount=" + participant.getShareAmount(), participant.getPayeeUser());
    }

    @Scheduled(cron = "0 */15 * * * *")
    @SchedulerLock(name = "split-expiry", lockAtMostFor = "PT5M")
    @Transactional
    public void expireOverdue() {
        try {
            List<SplitRequest> overdue = splitRequestRepository.findByStatusAndExpiresAtBefore(
                    SplitRequest.SplitStatus.PENDING, LocalDateTime.now());
            for (SplitRequest sr : overdue) {
                sr.setStatus(SplitRequest.SplitStatus.EXPIRED);
                splitRequestRepository.save(sr);
                log.info("Expired split request {}", sr.getId());
            }
        } catch (Exception e) {
            log.error("Error expiring overdue splits", e);
        }
    }
}
