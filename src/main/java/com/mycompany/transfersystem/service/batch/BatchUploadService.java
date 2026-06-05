package com.mycompany.transfersystem.service.batch;

import com.mycompany.transfersystem.entity.BatchJob;
import com.mycompany.transfersystem.entity.BatchJobRow;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.event.financial.FinancialWorkflowEvents;
import com.mycompany.transfersystem.repository.BatchJobRepository;
import com.mycompany.transfersystem.repository.BatchJobRowRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import com.mycompany.transfersystem.service.AuditService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class BatchUploadService {

    private final BatchJobRepository batchJobRepository;
    private final BatchJobRowRepository batchJobRowRepository;
    private final BatchValidationService batchValidationService;
    private final WalletRepository walletRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public BatchUploadService(BatchJobRepository batchJobRepository,
                              BatchJobRowRepository batchJobRowRepository,
                              BatchValidationService batchValidationService,
                              WalletRepository walletRepository,
                              AuditService auditService,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.batchJobRepository = batchJobRepository;
        this.batchJobRowRepository = batchJobRowRepository;
        this.batchValidationService = batchValidationService;
        this.walletRepository = walletRepository;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public com.mycompany.transfersystem.dto.batch.BatchUploadResponse upload(MultipartFile file, User submitter, boolean validateOnly) {
        List<String> errors = new ArrayList<>();
        List<BatchJobRow> rows = new ArrayList<>();
        BigDecimal totalUsd = BigDecimal.ZERO;
        int rowNum = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 2) {
                    errors.add("Row " + rowNum + ": need at least receiver,amount");
                    continue;
                }
                String receiver = parts[0].trim();
                String amountStr = parts.length > 1 ? parts[1].trim() : "0";
                String currency = parts.length > 2 ? parts[2].trim() : "USD";
                String description = parts.length > 3 ? parts[3].trim() : "";
                if (currency.isBlank()) currency = "USD";
                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountStr);
                } catch (NumberFormatException e) {
                    errors.add("Row " + rowNum + ": invalid amount " + amountStr);
                    continue;
                }
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Row " + rowNum + ": amount must be positive");
                    continue;
                }
                if (!validateOnly && batchValidationService.resolveReceiverWalletId(receiver).isEmpty()) {
                    errors.add("Row " + rowNum + ": receiver not found: " + receiver);
                    continue;
                }
                totalUsd = totalUsd.add(amount);
                BatchJobRow row = BatchJobRow.builder()
                        .rowNumber(rowNum)
                        .receiverIdentifier(receiver)
                        .amount(amount)
                        .currency(currency)
                        .description(description)
                        .status("PENDING")
                        .build();
                rows.add(row);
            }
        } catch (Exception e) {
            errors.add("Parse error: " + e.getMessage());
            return com.mycompany.transfersystem.dto.batch.BatchUploadResponse.builder()
                    .jobId(0L)
                    .totalRows(rowNum)
                    .validRows(0)
                    .invalidRows(rowNum)
                    .estimatedTotalUsd(BigDecimal.ZERO)
                    .validationErrors(errors)
                    .build();
        }
        if (validateOnly) {
            return com.mycompany.transfersystem.dto.batch.BatchUploadResponse.builder()
                    .jobId(0L)
                    .totalRows(rowNum)
                    .validRows(rows.size())
                    .invalidRows(rowNum - rows.size())
                    .estimatedTotalUsd(totalUsd)
                    .validationErrors(errors)
                    .build();
        }
        Wallet sourceWallet = walletRepository.findByUser_Id(submitter.getId()).orElse(null);
        BatchJob job = BatchJob.builder()
                .submittedBy(submitter)
                .batchType("CSV_UPLOAD")
                .filename(file.getOriginalFilename())
                .totalRows(rows.size())
                .totalAmountUsd(totalUsd)
                .status("PENDING_APPROVAL")
                .sourceWallet(sourceWallet)
                .build();
        job = batchJobRepository.save(job);
        for (BatchJobRow row : rows) {
            row.setBatchJob(job);
            batchJobRowRepository.save(row);
        }
        auditService.log("BATCH_JOB_SUBMITTED", "BATCH_JOB", job.getId(), "rows=" + rows.size() + " total=" + totalUsd, submitter);
        applicationEventPublisher.publishEvent(new FinancialWorkflowEvents.BatchJobSubmittedEvent(
                job.getId(), submitter.getId(), rows.size(), totalUsd, true));
        return com.mycompany.transfersystem.dto.batch.BatchUploadResponse.builder()
                .jobId(job.getId())
                .totalRows(rows.size())
                .validRows(rows.size())
                .invalidRows(errors.size())
                .estimatedTotalUsd(totalUsd)
                .validationErrors(errors.isEmpty() ? null : errors)
                .build();
    }
}
