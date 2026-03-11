package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.QrGenerationResponse;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.mycompany.transfersystem.dto.QrGenerationResponse;
import com.mycompany.transfersystem.entity.QrToken;
import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.QrTokenRepository;
import com.mycompany.transfersystem.repository.TransactionRepository;
import com.mycompany.transfersystem.util.QrEncryptionUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class BarcodeGenerationService {

    private static final Duration QR_TTL = Duration.ofMinutes(30);

    private final QrTokenRepository qrTokenRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;
    private final TotpService totpService;
    private final QrEncryptionUtil qrEncryptionUtil;
    private final NotificationService notificationService;

    public BarcodeGenerationService(QrTokenRepository qrTokenRepository,
                                   TransactionRepository transactionRepository,
                                   AuditService auditService,
                                   TotpService totpService,
                                   QrEncryptionUtil qrEncryptionUtil,
                                   NotificationService notificationService) {
        this.qrTokenRepository = qrTokenRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
        this.totpService = totpService;
        this.qrEncryptionUtil = qrEncryptionUtil;
        this.notificationService = notificationService;
    }

    @Transactional
    public QrGenerationResponse generateForTransaction(Long transactionId, User requestingUser) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        if (tx.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("QR can only be generated for PENDING transactions");
        }

        qrTokenRepository.findActiveByTransactionId(transactionId)
                .ifPresent(old -> {
                    old.setUsed(true);
                    qrTokenRepository.save(old);
                });

        String totpSecret = totpService.generateSecret();
        String releasePasscode = tx.getReleasePasscode() != null ? tx.getReleasePasscode() : "";
        String rawPayload = transactionId + "|" + releasePasscode + "|" + totpSecret;
        String encryptedPayload = qrEncryptionUtil.encrypt(rawPayload);
        String tokenHash = DigestUtils.sha256Hex(encryptedPayload);

        Instant expiresAt = Instant.now().plus(QR_TTL);

        QrToken qrToken = QrToken.builder()
                .transaction(tx)
                .tokenHash(tokenHash)
                .totpSecret(qrEncryptionUtil.encrypt(totpSecret))
                .expiresAt(expiresAt)
                .build();
        qrTokenRepository.save(qrToken);

        String qrImageBase64 = encodeQrToPng(encryptedPayload, 400);

        auditService.log("QR_GENERATED", "QrToken", qrToken.getId(),
                "Generated QR for transaction " + transactionId, requestingUser);

        QrGenerationResponse response = QrGenerationResponse.builder()
                .qrImageBase64(qrImageBase64)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .transactionId(transactionId)
                .build();
        if (notificationService != null) {
            notificationService.notifyQrReady(tx, response);
        }
        return response;
    }

    private String encodeQrToPng(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException e) {
            throw new RuntimeException("QR encoding failed", e);
        }
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireStaleTokens() {
        int expired = qrTokenRepository.expireStaleTokens(Instant.now());
        if (expired > 0) {
            // Log via system if needed
        }
    }
}
