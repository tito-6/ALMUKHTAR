package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.annotation.RequireIdempotencyKey;
import com.mycompany.transfersystem.dto.batch.BatchUploadResponse;
import com.mycompany.transfersystem.entity.BatchJob;
import com.mycompany.transfersystem.entity.BatchJobRow;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.BatchJobRepository;
import com.mycompany.transfersystem.repository.BatchJobRowRepository;
import com.mycompany.transfersystem.repository.BatchTemplateRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.batch.BatchExecutionService;
import com.mycompany.transfersystem.service.batch.BatchProgressService;
import com.mycompany.transfersystem.service.batch.BatchUploadService;
import com.mycompany.transfersystem.util.SecurityUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/batch")
@CrossOrigin(origins = "*")
public class BatchTransferController {

    private final BatchUploadService batchUploadService;
    private final BatchExecutionService batchExecutionService;
    private final BatchProgressService batchProgressService;
    private final BatchJobRepository batchJobRepository;
    private final BatchJobRowRepository batchJobRowRepository;
    private final BatchTemplateRepository templateRepository;
    private final UserRepository userRepository;

    public BatchTransferController(BatchUploadService batchUploadService,
                                   BatchExecutionService batchExecutionService,
                                   BatchProgressService batchProgressService,
                                   BatchJobRepository batchJobRepository,
                                   BatchJobRowRepository batchJobRowRepository,
                                   BatchTemplateRepository templateRepository,
                                   UserRepository userRepository) {
        this.batchUploadService = batchUploadService;
        this.batchExecutionService = batchExecutionService;
        this.batchProgressService = batchProgressService;
        this.batchJobRepository = batchJobRepository;
        this.batchJobRowRepository = batchJobRowRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('CORPORATE_ADMIN','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<BatchUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        BatchUploadResponse res = batchUploadService.upload(file, user, false);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/upload/validate-only")
    @PreAuthorize("hasRole('CORPORATE_ADMIN')")
    public ResponseEntity<BatchUploadResponse> validateOnly(@RequestParam("file") MultipartFile file,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        BatchUploadResponse res = batchUploadService.upload(file, user, true);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MOTHER_BRANCH_ADMIN','PLATFORM_OWNER')")
    public ResponseEntity<BatchJob> approve(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = SecurityUtils.resolveUser(userDetails, userRepository);
        BatchJob job = batchJobRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Batch job not found"));
        if (!"PENDING_APPROVAL".equals(job.getStatus())) {
            throw new IllegalStateException("Job is not PENDING_APPROVAL");
        }
        job.setStatus("APPROVED");
        job.setApprovedBy(admin);
        job.setApprovedAt(java.time.Instant.now());
        job = batchJobRepository.save(job);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{id}/execute")
    @RequireIdempotencyKey
    @PreAuthorize("hasRole('MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Void> execute(@PathVariable Long id) {
        batchExecutionService.executeBatch(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping(value = "/{id}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter progress(@PathVariable Long id) {
        return batchProgressService.subscribe(id);
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('CORPORATE_ADMIN','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> report(@PathVariable Long id) {
        BatchJob job = batchJobRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Batch job not found"));
        List<BatchJobRow> rows = batchJobRowRepository.findByBatchJob_IdOrderByRowNumber(id);
        Map<String, Object> report = new HashMap<>();
        report.put("jobId", job.getId());
        report.put("status", job.getStatus());
        report.put("totalRows", job.getTotalRows());
        report.put("successCount", job.getSuccessCount());
        report.put("failedCount", job.getFailedCount());
        report.put("rows", rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("rowNumber", r.getRowNumber());
            m.put("receiverIdentifier", r.getReceiverIdentifier());
            m.put("amount", r.getAmount());
            m.put("status", r.getStatus());
            m.put("errorMessage", r.getErrorMessage());
            m.put("transactionId", r.getTransactionId() != null ? r.getTransactionId().getId() : null);
            return m;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(report);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CORPORATE_ADMIN','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<List<BatchJob>> listMyBatches(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(batchJobRepository.findBySubmittedBy_IdOrderByCreatedAtDesc(user.getId()));
    }

    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CORPORATE_ADMIN','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<BatchJob> cancel(@PathVariable Long id) {
        BatchJob job = batchJobRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Batch job not found"));
        if (!"PENDING_APPROVAL".equals(job.getStatus()) && !"APPROVED".equals(job.getStatus())) {
            throw new IllegalStateException("Job cannot be cancelled");
        }
        job.setStatus("CANCELLED");
        job = batchJobRepository.save(job);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/templates")
    @PreAuthorize("hasRole('CORPORATE_ADMIN')")
    public ResponseEntity<List<com.mycompany.transfersystem.entity.BatchTemplate>> getTemplates(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(templateRepository.findByOwnerUser_Id(user.getId()));
    }
}
