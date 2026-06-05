package com.mycompany.transfersystem.service.batch;

import com.mycompany.transfersystem.entity.BatchJob;
import com.mycompany.transfersystem.entity.BatchJobRow;
import com.mycompany.transfersystem.entity.WalletTransaction;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.repository.BatchJobRepository;
import com.mycompany.transfersystem.repository.BatchJobRowRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class BatchExecutionService {

    private static final Logger log = LoggerFactory.getLogger(BatchExecutionService.class);

    private final BatchJobRepository batchJobRepository;
    private final BatchJobRowRepository batchJobRowRepository;
    private final BatchValidationService batchValidationService;
    private final WalletService walletService;
    private final BatchProgressService batchProgressService;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public BatchExecutionService(BatchJobRepository batchJobRepository,
                                 BatchJobRowRepository batchJobRowRepository,
                                 BatchValidationService batchValidationService,
                                 WalletService walletService,
                                 BatchProgressService batchProgressService,
                                 AuditService auditService,
                                 ApplicationEventPublisher applicationEventPublisher) {
        this.batchJobRepository = batchJobRepository;
        this.batchJobRowRepository = batchJobRowRepository;
        this.batchValidationService = batchValidationService;
        this.walletService = walletService;
        this.batchProgressService = batchProgressService;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Async("batchExecutor")
    @Transactional
    public void executeBatch(Long batchJobId) {
        BatchJob job = batchJobRepository.findById(batchJobId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch job not found"));
        if (!"APPROVED".equals(job.getStatus())) {
            log.warn("Batch job {} not APPROVED, status={}", batchJobId, job.getStatus());
            return;
        }
        if (job.getSourceWallet() == null) {
            job.setStatus("FAILED");
            batchJobRepository.save(job);
            batchProgressService.complete(batchJobId);
            return;
        }
        job.setStatus("PROCESSING");
        batchJobRepository.save(job);

        List<BatchJobRow> rows = batchJobRowRepository.findByBatchJob_IdOrderByRowNumber(batchJobId);
        Long senderWalletId = job.getSourceWallet().getId();
        for (BatchJobRow row : rows) {
            if (!"PENDING".equals(row.getStatus())) continue;
            processOneRow(batchJobId, job, row, senderWalletId);
            job = batchJobRepository.findById(batchJobId).orElse(job);
            double pct = job.getTotalRows() > 0 ? (100.0 * job.getProcessedRows() / job.getTotalRows()) : 0;
            batchProgressService.broadcast(batchJobId, com.mycompany.transfersystem.dto.batch.BatchProgressResponse.builder()
                    .jobId(batchJobId)
                    .processedRows(job.getProcessedRows())
                    .successCount(job.getSuccessCount())
                    .failedCount(job.getFailedCount())
                    .percentComplete(pct)
                    .estimatedRemaining("")
                    .build());
        }
        job.setStatus("COMPLETED");
        job.setCompletedAt(Instant.now());
        batchJobRepository.save(job);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.BatchJobCompletedEvent(
                job.getId(),
                job.getSubmittedBy() != null ? job.getSubmittedBy().getId() : null,
                job.getSuccessCount(),
                job.getFailedCount()));
        batchProgressService.complete(batchJobId);
        auditService.log("BATCH_JOB_COMPLETED", "BATCH_JOB", job.getId(),
                "success=" + job.getSuccessCount() + " failed=" + job.getFailedCount(), job.getSubmittedBy());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOneRow(Long batchJobId, BatchJob job, BatchJobRow row, Long senderWalletId) {
        try {
            Optional<Long> receiverWalletIdOpt = batchValidationService.resolveReceiverWalletId(row.getReceiverIdentifier());
            if (receiverWalletIdOpt.isEmpty()) {
                row.setStatus("FAILED");
                row.setErrorMessage("Receiver not found: " + row.getReceiverIdentifier());
                row.setProcessedAt(Instant.now());
                batchJobRowRepository.save(row);
                job.setFailedCount(job.getFailedCount() + 1);
            } else {
                WalletTransaction tx = walletService.transferP2P(
                        senderWalletId,
                        receiverWalletIdOpt.get(),
                        row.getCurrency(),
                        row.getAmount(),
                        "BATCH-" + batchJobId + "-R" + row.getRowNumber(),
                        row.getDescription() != null ? row.getDescription() : "Batch transfer");
                row.setStatus("SUCCESS");
                row.setTransactionId(tx);
                row.setProcessedAt(Instant.now());
                batchJobRowRepository.save(row);
                job.setSuccessCount(job.getSuccessCount() + 1);
            }
        } catch (Exception e) {
            row.setStatus("FAILED");
            row.setErrorMessage(e.getMessage());
            row.setProcessedAt(Instant.now());
            batchJobRowRepository.save(row);
            job.setFailedCount(job.getFailedCount() + 1);
        }
        job.setProcessedRows(job.getProcessedRows() + 1);
        batchJobRepository.save(job);
    }
}
