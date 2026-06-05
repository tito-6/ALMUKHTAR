package com.mycompany.transfersystem.service.escrow;

import com.mycompany.transfersystem.entity.EscrowContract;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.enums.WalletTransactionType;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.exception.ConditionNotMetException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.WalletRepository;
import com.mycompany.transfersystem.repository.EscrowContractRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.accounting.AccountingLedgerService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class EscrowWorkflowService {

    private final EscrowContractRepository escrowContractRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final AccountingLedgerService accountingLedgerService;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public EscrowWorkflowService(EscrowContractRepository escrowContractRepository,
                                 WalletRepository walletRepository,
                                 WalletService walletService,
                                 AccountingLedgerService accountingLedgerService,
                                 AuditService auditService,
                                 ApplicationEventPublisher applicationEventPublisher) {
        this.escrowContractRepository = escrowContractRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.accountingLedgerService = accountingLedgerService;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public EscrowContract fund(Long contractId, User actor) {
        EscrowContract c = lockedContract(contractId);
        requireActor(c.getInitiatorUser(), actor, "Only the escrow initiator can fund this contract");
        requireStatus(c, "ACTIVE", "Only active escrow contracts can be funded");

        Wallet initiatorWallet = walletRepository.findByUser_Id(c.getInitiatorUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Initiator wallet not found"));
        walletService.debit(initiatorWallet.getId(), c.getCurrency(), c.getAmount(),
                WalletTransactionType.ESCROW_FUND, "ESCROW-FUND-" + c.getId(),
                "Escrow contract funded");
        accountingLedgerService.postDoubleEntryForReference(
                "ESCROW_CONTRACT", c.getId(),
                "WALLET_" + initiatorWallet.getId() + "_" + c.getCurrency(),
                "ESCROW_PAYABLE_" + c.getId(),
                c.getAmount(), c.getCurrency(),
                "Escrow funds held", actor);

        c.setStatus("FUNDED");
        c.setFundedAt(Instant.now());
        c = escrowContractRepository.save(c);
        auditService.log("ESCROW_FUNDED", "ESCROW_CONTRACT", c.getId(),
                "amount=" + c.getAmount() + " " + c.getCurrency(), actor);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.EscrowFundedEvent(
                c.getId(), c.getInitiatorUser().getId(), c.getBeneficiaryUser().getId(),
                c.getAmount(), c.getCurrency(), c.getConditionType()));
        return c;
    }

    @Transactional
    public EscrowContract release(Long contractId, User actor) {
        EscrowContract c = lockedContract(contractId);
        if (!isActor(c.getInitiatorUser(), actor) && !isOpsActor(actor)) {
            throw new AccessDeniedException("Only the escrow initiator or an authorized operator can release this contract");
        }
        requireStatus(c, "FUNDED", "Only funded escrow contracts can be released");

        Wallet beneficiaryWallet = walletRepository.findByUser_Id(c.getBeneficiaryUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary wallet not found"));
        walletService.credit(beneficiaryWallet.getId(), c.getCurrency(), c.getAmount(),
                WalletTransactionType.ESCROW_RELEASE, "ESCROW-REL-" + c.getId(),
                "Escrow contract released");
        accountingLedgerService.postDoubleEntryForReference(
                "ESCROW_RELEASE", c.getId(),
                "ESCROW_PAYABLE_" + c.getId(),
                "WALLET_" + beneficiaryWallet.getId() + "_" + c.getCurrency(),
                c.getAmount(), c.getCurrency(),
                "Escrow funds released", actor);

        c.setStatus("RELEASED");
        c.setReleasedAt(Instant.now());
        c = escrowContractRepository.save(c);
        auditService.log("ESCROW_RELEASED", "ESCROW_CONTRACT", c.getId(),
                "amount=" + c.getAmount() + " " + c.getCurrency(), actor);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.EscrowReleasedEvent(
                c.getId(), c.getInitiatorUser().getId(), c.getBeneficiaryUser().getId(),
                c.getAmount(), c.getCurrency()));
        return c;
    }

    @Transactional
    public EscrowContract dispute(Long contractId, User actor, String reasonCategory) {
        EscrowContract c = lockedContract(contractId);
        if (!isActor(c.getInitiatorUser(), actor) && !isActor(c.getBeneficiaryUser(), actor) && !isOpsActor(actor)) {
            throw new AccessDeniedException("Only an escrow party or authorized operator can dispute this contract");
        }
        if (!"ACTIVE".equals(c.getStatus()) && !"FUNDED".equals(c.getStatus())) {
            throw new ConditionNotMetException("Only active or funded escrow contracts can be disputed");
        }
        c.setStatus("DISPUTED");
        c = escrowContractRepository.save(c);
        auditService.log("ESCROW_DISPUTED", "ESCROW_CONTRACT", c.getId(),
                "reason=" + reasonCategory, actor);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.EscrowDisputedEvent(
                c.getId(), c.getInitiatorUser().getId(), c.getBeneficiaryUser().getId(),
                reasonCategory, c.getConditionType()));
        return c;
    }

    @Deprecated
    @Transactional
    public EscrowContract markFunded(Long contractId) {
        EscrowContract c = escrowContractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow not found: " + contractId));
        c.setStatus("FUNDED");
        c.setFundedAt(Instant.now());
        c = escrowContractRepository.save(c);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.EscrowFundedEvent(
                c.getId(), c.getInitiatorUser().getId(), c.getBeneficiaryUser().getId(),
                c.getAmount(), c.getCurrency(), c.getConditionType()));
        return c;
    }

    @Deprecated
    @Transactional
    public EscrowContract markReleased(Long contractId) {
        EscrowContract c = escrowContractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow not found: " + contractId));
        c.setStatus("RELEASED");
        c.setReleasedAt(Instant.now());
        c = escrowContractRepository.save(c);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.EscrowReleasedEvent(
                c.getId(), c.getInitiatorUser().getId(), c.getBeneficiaryUser().getId(),
                c.getAmount(), c.getCurrency()));
        return c;
    }

    @Deprecated
    @Transactional
    public EscrowContract markDisputed(Long contractId, User actor, String reasonCategory) {
        EscrowContract c = escrowContractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow not found: " + contractId));
        c.setStatus("DISPUTED");
        c = escrowContractRepository.save(c);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.EscrowDisputedEvent(
                c.getId(), c.getInitiatorUser().getId(), c.getBeneficiaryUser().getId(),
                reasonCategory, c.getConditionType()));
        return c;
    }

    private EscrowContract lockedContract(Long contractId) {
        return escrowContractRepository.findByIdForUpdate(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow not found: " + contractId));
    }

    private void requireActor(User expected, User actual, String message) {
        if (!isActor(expected, actual)) {
            throw new AccessDeniedException(message);
        }
    }

    private boolean isActor(User expected, User actual) {
        return expected != null && actual != null && expected.getId().equals(actual.getId());
    }

    private void requireStatus(EscrowContract contract, String expected, String message) {
        if (!expected.equals(contract.getStatus())) {
            throw new ConditionNotMetException(message);
        }
    }

    private boolean isOpsActor(User actor) {
        return actor != null && switch (actor.getRole()) {
            case PLATFORM_OWNER, SUPER_ADMIN, MOTHER_BRANCH_ADMIN, BRANCH_MANAGER, AUDITOR -> true;
            default -> false;
        };
    }
}
