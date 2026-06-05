package com.mycompany.transfersystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.dto.*;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.SyncQueue;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.SyncStatus;
import com.mycompany.transfersystem.exception.ConditionNotMetException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.CashierShiftRepository;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.SyncQueueRepository;
import com.mycompany.transfersystem.util.QrEncryptionUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class SyncQueueService {

    private enum ApplyOutcome {
        APPLIED, CONFLICT, REJECTED
    }

    private static final Logger log = LoggerFactory.getLogger(SyncQueueService.class);

    private final SyncQueueRepository syncQueueRepository;
    private final TransactionService transactionService;
    private final FundRepository fundRepository;
    private final AuditService auditService;
    private final QrEncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;
    private final CashierShiftRepository cashierShiftRepository;

    @Value("${offline.device.hmac-secret:}")
    private String deviceHmacSecret;

    @Value("${offline.sync.max-applied-per-day:500}")
    private int maxAppliedPerDay;

    @Value("${offline.sync.max-amount-per-tx:1000000}")
    private BigDecimal maxAmountPerTx;

    public SyncQueueService(SyncQueueRepository syncQueueRepository,
                            TransactionService transactionService,
                            FundRepository fundRepository,
                            AuditService auditService,
                            QrEncryptionUtil encryptionUtil,
                            ObjectMapper objectMapper,
                            CashierShiftRepository cashierShiftRepository) {
        this.syncQueueRepository = syncQueueRepository;
        this.transactionService = transactionService;
        this.fundRepository = fundRepository;
        this.auditService = auditService;
        this.encryptionUtil = encryptionUtil;
        this.objectMapper = objectMapper;
        this.cashierShiftRepository = cashierShiftRepository;
    }

    @Transactional
    public SyncQueueResponse enqueue(OfflineTransactionRequest request, User cashier) {
        try {
            String idempotencyKey = resolveIdempotencyKey(request);
            request.setIdempotencyKey(idempotencyKey);

            var existing = syncQueueRepository.findByDeviceIdAndIdempotencyKey(request.getDeviceId(), idempotencyKey);
            if (existing.isPresent()) {
                SyncQueue item = existing.get();
                return SyncQueueResponse.builder()
                        .queueId(item.getId())
                        .status(item.getStatus().name())
                        .build();
            }

            String payloadJson = objectMapper.writeValueAsString(request);
            String encrypted = encryptionUtil.encrypt(payloadJson);
            String checksum = DigestUtils.sha256Hex(payloadJson);
            String signedHash = request.getSignedPayloadHash() != null && !request.getSignedPayloadHash().isBlank()
                    ? request.getSignedPayloadHash()
                    : checksum;
            String cashierDeviceId = request.getCashierDeviceId() != null && !request.getCashierDeviceId().isBlank()
                    ? request.getCashierDeviceId()
                    : request.getDeviceId();

            SyncQueue item = SyncQueue.builder()
                    .deviceId(request.getDeviceId())
                    .cashierDeviceId(cashierDeviceId)
                    .deviceSequenceNumber(request.getDeviceSequenceNumber())
                    .signedPayloadHash(signedHash)
                    .deviceSignature(request.getDeviceSignature())
                    .cashier(cashier)
                    .payloadEncrypted(encrypted)
                    .payloadChecksum(checksum)
                    .idempotencyKey(idempotencyKey)
                    .status(SyncStatus.PENDING)
                    .offlineTimestamp(request.getOfflineTimestamp())
                    .build();

            syncQueueRepository.save(item);
            auditService.log("OFFLINE_QUEUED", "SyncQueue", item.getId(),
                    "Device: " + cashierDeviceId, cashier);

            return SyncQueueResponse.builder().queueId(item.getId()).status("QUEUED").build();
        } catch (Exception e) {
            throw new ConditionNotMetException("Failed to enqueue offline transaction: " + e.getMessage());
        }
    }

    @Transactional
    public SyncResultReport processPendingForDevice(String deviceId, User cashier) {
        List<SyncQueue> pending = syncQueueRepository.findByDeviceIdAndStatusOrderByOfflineTimestampAsc(deviceId, SyncStatus.PENDING);
        int applied = 0, conflicts = 0, rejected = 0;
        List<String> errors = new ArrayList<>();

        for (SyncQueue item : pending) {
            try {
                ApplyOutcome outcome = applyOne(item, cashier, false, false);
                switch (outcome) {
                    case APPLIED -> applied++;
                    case CONFLICT -> conflicts++;
                    case REJECTED -> rejected++;
                }
            } catch (Exception e) {
                markRejected(item, e.getMessage());
                errors.add("Queue #" + item.getId() + ": " + e.getMessage());
                rejected++;
                log.error("Failed to apply sync queue item {}: {}", item.getId(), e.getMessage());
            }
        }

        return SyncResultReport.builder()
                .applied(applied)
                .conflicts(conflicts)
                .rejected(rejected)
                .errors(errors)
                .processedAt(Instant.now())
                .build();
    }

    @Transactional
    public void resolveConflict(Long queueId, User manager, com.mycompany.transfersystem.dto.sync.ResolveSyncConflictRequest req) {
        SyncQueue item = syncQueueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Sync queue item not found: " + queueId));
        if (item.getStatus() != SyncStatus.CONFLICT) {
            throw new ConditionNotMetException("Item is not in CONFLICT status");
        }
        if (req.getDecision() == com.mycompany.transfersystem.dto.sync.ResolveSyncConflictRequest.Decision.DISCARD) {
            item.setStatus(SyncStatus.REJECTED);
            item.setConflictStatus("MANAGER_DISCARDED");
            item.setConflictReason(req.getManagerNote());
            item.setSyncedAt(Instant.now());
            syncQueueRepository.save(item);
            auditService.log("OFFLINE_CONFLICT_DISCARDED", "SyncQueue", queueId, req.getManagerNote(), manager);
            return;
        }
        try {
            ApplyOutcome outcome = applyOne(item, manager, true, true);
            if (outcome != ApplyOutcome.APPLIED) {
                throw new ConditionNotMetException("Could not apply after manager review: " + outcome);
            }
            item.setConflictStatus("MANAGER_APPLIED");
            syncQueueRepository.save(item);
            auditService.log("OFFLINE_CONFLICT_APPLIED", "SyncQueue", queueId, req.getManagerNote(), manager);
        } catch (Exception e) {
            throw new ConditionNotMetException(e.getMessage());
        }
    }

    private ApplyOutcome applyOne(SyncQueue item, User actor, boolean managerOverrideDuplicate, boolean skipShiftCheck) throws Exception {
        String decrypted = encryptionUtil.decrypt(item.getPayloadEncrypted());
        if (!DigestUtils.sha256Hex(decrypted).equals(item.getPayloadChecksum())) {
            markRejected(item, "Checksum mismatch");
            return ApplyOutcome.REJECTED;
        }

        OfflineTransactionRequest req = objectMapper.readValue(decrypted, OfflineTransactionRequest.class);
        String idemKey = item.getIdempotencyKey() != null ? item.getIdempotencyKey() : resolveIdempotencyKey(req);
        String effectiveDevice = item.getCashierDeviceId() != null ? item.getCashierDeviceId() : item.getDeviceId();

        if (item.getSignedPayloadHash() != null && !item.getSignedPayloadHash().isBlank()) {
            String actual = DigestUtils.sha256Hex(decrypted);
            if (!actual.equalsIgnoreCase(item.getSignedPayloadHash())) {
                markConflict(item, "signedPayloadHash mismatch");
                return ApplyOutcome.CONFLICT;
            }
        }

        if (deviceHmacSecret != null && !deviceHmacSecret.isBlank()) {
            if (item.getDeviceSignature() == null || item.getSignedPayloadHash() == null) {
                markConflict(item, "Missing device signature");
                return ApplyOutcome.CONFLICT;
            }
            if (!verifyHmac(deviceHmacSecret, item.getSignedPayloadHash(), item.getDeviceSignature())) {
                markConflict(item, "Invalid device signature");
                return ApplyOutcome.CONFLICT;
            }
        }

        if (req.getAmount() != null && req.getAmount().compareTo(maxAmountPerTx) > 0) {
            markConflict(item, "Offline amount exceeds configured max per transaction");
            return ApplyOutcome.CONFLICT;
        }

        if (item.getDeviceSequenceNumber() != null) {
            long maxApplied = syncQueueRepository.findMaxAppliedDeviceSequence(effectiveDevice, SyncStatus.APPLIED).orElse(-1L);
            if (item.getDeviceSequenceNumber() <= maxApplied && !managerOverrideDuplicate) {
                markConflict(item, "Device sequence replay or reorder");
                return ApplyOutcome.CONFLICT;
            }
        }

        if (!skipShiftCheck) {
            Instant dayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
            long appliedToday = syncQueueRepository.countAppliedSince(actor.getId(), SyncStatus.APPLIED, dayStart);
            if (appliedToday >= maxAppliedPerDay) {
                markConflict(item, "Offline daily apply limit exceeded for cashier");
                return ApplyOutcome.CONFLICT;
            }

            if (cashierShiftRepository.findByCashierIdAndStatus(actor.getId(),
                    com.mycompany.transfersystem.entity.CashierShift.ShiftStatus.OPEN).isEmpty()) {
                markConflict(item, "No open cashier shift for offline apply");
                return ApplyOutcome.CONFLICT;
            }
        }

        if (!managerOverrideDuplicate && syncQueueRepository.existsByDeviceIdAndIdempotencyKeyAndStatusAndIdNot(
                item.getDeviceId(), idemKey, SyncStatus.APPLIED, item.getId())) {
            markConflict(item, "Duplicate transaction");
            return ApplyOutcome.CONFLICT;
        }

        Fund fund = fundRepository.findById(req.getFundId())
                .orElseThrow(() -> new ResourceNotFoundException("Fund not found"));
        if (fund.getBalance().compareTo(req.getAmount()) < 0) {
            markConflict(item, "Insufficient funds at sync time");
            return ApplyOutcome.CONFLICT;
        }

        TransferRequest tr = new TransferRequest();
        tr.setSenderId(req.getSenderId());
        tr.setReceiverId(req.getReceiverId());
        tr.setFundId(req.getFundId());
        tr.setAmount(req.getAmount());
        transactionService.createTransfer(tr);

        item.setStatus(SyncStatus.APPLIED);
        item.setSyncedAt(Instant.now());
        item.setConflictReason(null);
        item.setConflictStatus(null);
        syncQueueRepository.save(item);
        auditService.log("OFFLINE_SYNC_APPLIED", "SyncQueue", item.getId(),
                "Applied offline tx device " + effectiveDevice, actor);
        return ApplyOutcome.APPLIED;
    }

    public List<SyncQueueResponse> getStatusForDevice(String deviceId) {
        return syncQueueRepository.findByDeviceIdAndStatusOrderByOfflineTimestampAsc(deviceId, SyncStatus.PENDING)
                .stream()
                .map(sq -> SyncQueueResponse.builder().queueId(sq.getId()).status(sq.getStatus().name()).build())
                .toList();
    }

    private void markConflict(SyncQueue item, String reason) {
        item.setStatus(SyncStatus.CONFLICT);
        item.setConflictReason(reason);
        item.setConflictStatus("OPEN");
        item.setSyncedAt(Instant.now());
        syncQueueRepository.save(item);
    }

    private void markRejected(SyncQueue item, String reason) {
        item.setStatus(SyncStatus.REJECTED);
        item.setConflictReason(reason);
        item.setSyncedAt(Instant.now());
        syncQueueRepository.save(item);
    }

    private String resolveIdempotencyKey(OfflineTransactionRequest request) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            return request.getIdempotencyKey();
        }
        String material = String.join("|",
                nullToEmpty(request.getDeviceId()),
                nullToEmpty(request.getCashierDeviceId()),
                String.valueOf(request.getFundId()),
                String.valueOf(request.getSenderId()),
                String.valueOf(request.getReceiverId()),
                request.getAmount() != null ? request.getAmount().stripTrailingZeros().toPlainString() : "",
                String.valueOf(request.getOfflineTimestamp()),
                String.valueOf(request.getDeviceSequenceNumber()));
        return DigestUtils.sha256Hex(material).substring(0, 32);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean verifyHmac(String secret, String message, String expectedHex) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return expectedHex.equalsIgnoreCase(Hex.encodeHexString(out));
        } catch (Exception e) {
            return false;
        }
    }
}
