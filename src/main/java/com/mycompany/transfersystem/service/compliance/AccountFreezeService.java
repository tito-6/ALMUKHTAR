package com.mycompany.transfersystem.service.compliance;

import com.mycompany.transfersystem.dto.freeze.CreateAccountFreezeCaseRequest;
import com.mycompany.transfersystem.dto.freeze.PublicFreezeCaseView;
import com.mycompany.transfersystem.entity.AccountFreezeCase;
import com.mycompany.transfersystem.entity.FreezeCaseEvent;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.AccountFreezeCaseRepository;
import com.mycompany.transfersystem.repository.FreezeCaseEventRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.NotificationService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountFreezeService {

    private final AccountFreezeCaseRepository caseRepository;
    private final FreezeCaseEventRepository eventRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Value("${app.freeze.dispute-base-url:https://almukhtar.app/disputes?case=}")
    private String disputeBaseUrl;

    @Transactional
    public AccountFreezeCase openCase(CreateAccountFreezeCaseRequest req, User actor) {
        User subject = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getUserId()));
        String publicId = "FZ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        LocalDateTime sla = req.getSlaDueAt() != null ? req.getSlaDueAt() : LocalDateTime.now().plusHours(48);
        String appealUrl = req.getAppealOrDisputeUrl() != null && !req.getAppealOrDisputeUrl().isBlank()
                ? req.getAppealOrDisputeUrl()
                : disputeBaseUrl + publicId;

        AccountFreezeCase c = caseRepository.save(AccountFreezeCase.builder()
                .publicCaseId(publicId)
                .user(subject)
                .walletId(req.getWalletId())
                .customerCategory(req.getCustomerCategory())
                .internalReason(req.getInternalReason())
                .customerMessage(req.getCustomerMessage())
                .slaDueAt(sla)
                .appealOrDisputeUrl(appealUrl)
                .status(AccountFreezeCase.CaseStatus.OPEN)
                .build());

        eventRepository.save(FreezeCaseEvent.builder()
                .freezeCase(c)
                .eventType(FreezeCaseEvent.EventType.CREATED)
                .actor(actor)
                .internalNote(req.getInternalReason())
                .customerSafeSummary(req.getCustomerMessage())
                .build());

        if (req.getWalletId() != null) {
            walletService.freezeWallet(req.getWalletId());
        }

        auditService.log("ACCOUNT_FREEZE_CASE_OPENED", "ACCOUNT_FREEZE_CASE", c.getId(), publicId, actor);

        notificationService.notifyUserPhones(subject, "Account notice",
                "Reference " + publicId + ". " + req.getCustomerMessage());

        String internalLine = "Case " + publicId + " category=" + req.getCustomerCategory()
                + " user=" + subject.getId();
        notificationService.notifyUsersByRole(UserRole.MOTHER_BRANCH_ADMIN,
                "Internal: freeze case", internalLine + " | " + truncate(req.getInternalReason(), 400));
        notificationService.notifyUsersByRole(UserRole.AUDITOR,
                "Internal: freeze case", internalLine + " | " + truncate(req.getInternalReason(), 400));
        notificationService.notifyUsersByRole(UserRole.PLATFORM_OWNER,
                "Internal: freeze case", internalLine + " | " + truncate(req.getInternalReason(), 400));

        return c;
    }

    @Transactional(readOnly = true)
    public PublicFreezeCaseView getPublicView(String publicCaseId) {
        AccountFreezeCase c = caseRepository.findByPublicCaseId(publicCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));
        return PublicFreezeCaseView.builder()
                .publicCaseId(c.getPublicCaseId())
                .customerCategory(c.getCustomerCategory())
                .customerMessage(c.getCustomerMessage())
                .slaDueAt(c.getSlaDueAt())
                .appealOrDisputeUrl(c.getAppealOrDisputeUrl())
                .status(c.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public List<FreezeCaseEvent> listEventsForCase(Long caseId, User actor) {
        if (actor.getRole() != UserRole.MOTHER_BRANCH_ADMIN
                && actor.getRole() != UserRole.AUDITOR
                && actor.getRole() != UserRole.PLATFORM_OWNER) {
            throw new SecurityException("Forbidden");
        }
        return eventRepository.findByFreezeCase_IdOrderByCreatedAtAsc(caseId);
    }

    @Transactional
    public AccountFreezeCase releaseCase(Long caseId, User actor, String internalNote) {
        AccountFreezeCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
        c.setStatus(AccountFreezeCase.CaseStatus.RELEASED);
        caseRepository.save(c);
        if (c.getWalletId() != null) {
            walletService.unfreezeWallet(c.getWalletId());
        }
        eventRepository.save(FreezeCaseEvent.builder()
                .freezeCase(c)
                .eventType(FreezeCaseEvent.EventType.RELEASED)
                .actor(actor)
                .internalNote(internalNote)
                .customerSafeSummary("Your case was closed. Reference " + c.getPublicCaseId())
                .build());
        auditService.log("ACCOUNT_FREEZE_CASE_RELEASED", "ACCOUNT_FREEZE_CASE", caseId, c.getPublicCaseId(), actor);
        notificationService.notifyUserPhones(c.getUser(), "Account update",
                "Case " + c.getPublicCaseId() + " closed.");
        return c;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
