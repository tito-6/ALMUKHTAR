package com.mycompany.transfersystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.dto.*;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.SyncQueue;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.SyncStatus;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.SyncQueueRepository;
import com.mycompany.transfersystem.util.QrEncryptionUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class SyncQueueService {

    private static final Logger log = LoggerFactory.getLogger(SyncQueueService.class);

    private final SyncQueueRepository syncQueueRepository;
    private final TransactionService transactionService;
    private final FundRepository fundRepository;
    private final AuditService auditService;
    private final QrEncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;

    public SyncQueueService(SyncQueueRepository syncQueueRepository,
                            TransactionService transactionService,
                            FundRepository fundRepository,
                            AuditService auditService,
                            QrEncryptionUtil encryptionUtil,
                            ObjectMapper objectMapper) {
        this.syncQueueRepository = syncQueueRepository;
        this.transactionService = transactionService;
        this.fundRepository = fundRepository;
        this.auditService = auditService;
        this.encryptionUtil = encryptionUtil;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SyncQueueResponse enqueue(OfflineTransactionRequest request, User cashier) {
        try {
            String payloadJson = objectMapper.writeValueAsString(request);
            String encrypted = encryptionUtil.encrypt(payloadJson);
            String checksum = DigestUtils.sha256Hex(payloadJson);

            SyncQueue item = SyncQueue.builder()
                    .deviceId(request.getDeviceId())
                    .cashier(cashier)
                    .payloadEncrypted(encrypted)
                    .payloadChecksum(checksum)
                    .status(SyncStatus.PENDING)
                    .offlineTimestamp(request.getOfflineTimestamp())
                    .build();

            syncQueueRepository.save(item);
            auditService.log("OFFLINE_QUEUED", "SyncQueue", item.getId(),
                    "Device: " + request.getDeviceId(), cashier);

            return SyncQueueResponse.builder().queueId(item.getId()).status("QUEUED").build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to enqueue offline transaction", e);
        }
    }

    @Transactional
    public SyncResultReport processPendingForDevice(String deviceId, User cashier) {
        List<SyncQueue> pending = syncQueueRepository.findByDeviceIdAndStatusOrderByOfflineTimestampAsc(deviceId, SyncStatus.PENDING);
        int applied = 0, conflicts = 0, rejected = 0;
        List<String> errors = new ArrayList<>();

        for (SyncQueue item : pending) {
            try {
                String decrypted = encryptionUtil.decrypt(item.getPayloadEncrypted());
                if (!DigestUtils.sha256Hex(decrypted).equals(item.getPayloadChecksum())) {
                    markRejected(item, "Checksum mismatch");
                    rejected++;
                    continue;
                }

                OfflineTransactionRequest req = objectMapper.readValue(decrypted, OfflineTransactionRequest.class);
                String idemKey = req.getIdempotencyKey();
                if (idemKey != null && syncQueueRepository.existsByPayloadChecksumAndStatus(idemKey, SyncStatus.APPLIED)) {
                    markConflict(item, "Duplicate transaction");
                    conflicts++;
                    continue;
                }

                Fund fund = fundRepository.findById(req.getFundId())
                        .orElseThrow(() -> new ResourceNotFoundException("Fund not found"));
                if (fund.getBalance().compareTo(req.getAmount()) < 0) {
                    markConflict(item, "Insufficient funds at sync time");
                    conflicts++;
                    continue;
                }

                TransferRequest tr = new TransferRequest();
                tr.setSenderId(req.getSenderId());
                tr.setReceiverId(req.getReceiverId());
                tr.setFundId(req.getFundId());
                tr.setAmount(req.getAmount());
                transactionService.createTransfer(tr);

                item.setStatus(SyncStatus.APPLIED);
                item.setSyncedAt(Instant.now());
                syncQueueRepository.save(item);
                applied++;
                auditService.log("OFFLINE_SYNC_APPLIED", "SyncQueue", item.getId(),
                        "Applied offline tx from device " + deviceId, cashier);
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

    public List<SyncQueueResponse> getStatusForDevice(String deviceId) {
        return syncQueueRepository.findByDeviceIdAndStatusOrderByOfflineTimestampAsc(deviceId, SyncStatus.PENDING)
                .stream()
                .map(sq -> SyncQueueResponse.builder().queueId(sq.getId()).status(sq.getStatus().name()).build())
                .toList();
    }

    private void markConflict(SyncQueue item, String reason) {
        item.setStatus(SyncStatus.CONFLICT);
        item.setConflictReason(reason);
        item.setSyncedAt(Instant.now());
        syncQueueRepository.save(item);
    }

    private void markRejected(SyncQueue item, String reason) {
        item.setStatus(SyncStatus.REJECTED);
        item.setConflictReason(reason);
        item.setSyncedAt(Instant.now());
        syncQueueRepository.save(item);
    }
}
