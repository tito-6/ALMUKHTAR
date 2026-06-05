package com.mycompany.transfersystem.service.dispute;

import com.mycompany.transfersystem.dto.dispute.OpenDisputeRequest;
import com.mycompany.transfersystem.entity.Dispute;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.exception.ConditionNotMetException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.repository.DisputeRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Dispute openDispute(Long userId, OpenDisputeRequest dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Dispute dispute = Dispute.builder()
                .reporterUser(user)
                .transactionId(dto.getTransactionId())
                .category(Dispute.DisputeCategory.valueOf(dto.getCategory().toUpperCase()))
                .description(dto.getDescription())
                .slaDeadline(LocalDateTime.now().plusHours(72))
                .build();
        dispute = disputeRepository.save(dispute);
        auditService.log("DISPUTE_OPENED", "DISPUTE", dispute.getId(), "txId=" + dto.getTransactionId(), user);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.DisputeOpenedEvent(
                dispute.getId(), user.getId(), dto.getTransactionId(), dto.getCategory()));
        return dispute;
    }

    @Transactional
    public void assignDispute(Long disputeId, Long branchId, Long assignerId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
        dispute.setAssignedBranchId(branchId);
        dispute.setStatus(Dispute.DisputeStatus.UNDER_REVIEW);
        disputeRepository.save(dispute);
        var assigner = userRepository.findById(assignerId).orElse(null);
        auditService.log("DISPUTE_ASSIGNED", "DISPUTE", disputeId, "branchId=" + branchId, assigner);
    }

    @Transactional
    public void resolveDispute(Long disputeId, String resolution, Long resolverId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
        dispute.setStatus(Dispute.DisputeStatus.RESOLVED);
        dispute.setResolution(resolution);
        dispute.setResolvedBy(resolverId);
        dispute.setResolvedAt(Instant.now());
        dispute.setSlaMet(dispute.getResolvedAt().isBefore(
                dispute.getSlaDeadline().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        disputeRepository.save(dispute);
        var resolver = userRepository.findById(resolverId).orElse(null);
        auditService.log("DISPUTE_RESOLVED", "DISPUTE", disputeId, "slaMet=" + dispute.getSlaMet(), resolver);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.DisputeResolvedEvent(
                disputeId, dispute.getReporterUser().getId(), resolution));
    }

    @Transactional
    public void rejectDispute(Long disputeId, String reason, Long resolverId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
        dispute.setStatus(Dispute.DisputeStatus.REJECTED);
        dispute.setResolution(reason);
        dispute.setResolvedBy(resolverId);
        dispute.setResolvedAt(Instant.now());
        disputeRepository.save(dispute);
        var resolver = userRepository.findById(resolverId).orElse(null);
        auditService.log("DISPUTE_REJECTED", "DISPUTE", disputeId, null, resolver);
    }

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "dispute-sla", lockAtMostFor = "PT5M")
    @Transactional
    public void checkSlaDeadlines() {
        try {
            List<Dispute> overdue = disputeRepository.findBySlaDeadlineBefore(LocalDateTime.now());
            for (Dispute d : overdue) {
                d.setSlaMet(false);
                disputeRepository.save(d);
                log.warn("Dispute {} past SLA deadline — escalation needed", d.getId());
            }
        } catch (Exception e) {
            log.error("Error checking SLA deadlines", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Dispute> getMyDisputes(Long userId, Pageable pageable) {
        return disputeRepository.findByReporterUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Dispute> getAllDisputes(Pageable pageable) {
        return disputeRepository.findAll(pageable);
    }
}
