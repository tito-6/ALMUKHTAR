package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.ScanValidationRequest;
import com.mycompany.transfersystem.dto.ScanValidationResponse;
import com.mycompany.transfersystem.entity.QrToken;
import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.exception.SecurityViolationException;
import com.mycompany.transfersystem.repository.QrTokenRepository;
import com.mycompany.transfersystem.repository.TransactionRepository;
import com.mycompany.transfersystem.util.QrEncryptionUtil;
import com.mycompany.transfersystem.service.transfer.PayoutCompletionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ScannerValidationService {

    private final QrTokenRepository qrTokenRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;
    private final TotpService totpService;
    private final QrEncryptionUtil qrEncryptionUtil;
    private final PayoutCompletionService payoutCompletionService;

    public ScannerValidationService(QrTokenRepository qrTokenRepository,
                                   TransactionRepository transactionRepository,
                                   AuditService auditService,
                                   TotpService totpService,
                                   QrEncryptionUtil qrEncryptionUtil,
                                   PayoutCompletionService payoutCompletionService) {
        this.qrTokenRepository = qrTokenRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
        this.totpService = totpService;
        this.qrEncryptionUtil = qrEncryptionUtil;
        this.payoutCompletionService = payoutCompletionService;
    }

    @Transactional
    public ScanValidationResponse validateScan(ScanValidationRequest request, User cashier) {
        QrToken qrToken = qrTokenRepository
                .findByTokenHashAndUsedFalseAndExpiresAtAfter(request.getTokenHash(), Instant.now())
                .orElseThrow(() -> new SecurityViolationException("QR token invalid, expired, or already used"));

        String decrypted = qrEncryptionUtil.decrypt(request.getEncryptedPayload());
        String[] parts = decrypted.split("\\|");
        if (parts.length != 3) {
            throw new SecurityViolationException("Malformed QR payload");
        }

        Long transactionId = Long.parseLong(parts[0]);
        String passcodeInQr = parts[1];
        String totpSecretRaw = qrEncryptionUtil.decrypt(qrToken.getTotpSecret());

        if (!qrToken.getTransaction().getId().equals(transactionId)) {
            auditService.log("QR_TAMPER_ATTEMPT", "QrToken", qrToken.getId(),
                    "Payload transactionId mismatch — cashier: " + cashier.getId(), cashier);
            throw new SecurityViolationException("QR payload integrity check failed");
        }

        boolean totpValid = totpService.verifyCode(totpSecretRaw, request.getTotpCode(), 1);
        if (!totpValid) {
            throw new SecurityViolationException("TOTP code invalid");
        }

        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!passcodeInQr.equals(tx.getReleasePasscode())) {
            throw new SecurityViolationException("Release passcode mismatch");
        }

        qrToken.setUsed(true);
        qrToken.setScannedAt(Instant.now());
        qrToken.setScannedBy(cashier);
        qrTokenRepository.save(qrToken);

        Transaction released = payoutCompletionService.completeAfterQrScan(tx, cashier);

        auditService.log("QR_SCANNED_SUCCESS", "Transaction", transactionId,
                "Released by cashier " + cashier.getUsername() + " via QR scan", cashier);

        return ScanValidationResponse.builder()
                .success(true)
                .transactionId(transactionId)
                .receiverName(released.getReceiver().getUsername())
                .amount(released.getAmount())
                .message("Transaction released successfully")
                .build();
    }
}
