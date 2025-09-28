package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.TransactionSearchRequest;
import com.mycompany.transfersystem.entity.AuditLog;
import com.mycompany.transfersystem.entity.CommissionRate;
import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Get fund status for a specific branch
     * GET /api/audit/funds/{branchId}
     * Access: BRANCH_MANAGER (must match branchId) or SUPER_ADMIN
     */
    @GetMapping("/funds/{branchId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<Map<String, Object>> getBranchFundStatus(@PathVariable Long branchId) {
        try {
            Map<String, Object> fundStatus = auditService.getBranchFundStatus(branchId);
            return ResponseEntity.ok(fundStatus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Search transactions with complex filtering
     * GET /api/audit/transactions/search
     * Access: SUPER_ADMIN and AUDITOR role
     */
    @GetMapping("/transactions/search")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<Transaction>> searchTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long senderBranchId,
            @RequestParam(required = false) Long receiverBranchId,
            @RequestParam(required = false) String sourceCurrency,
            @RequestParam(required = false) String destinationCurrency,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Long receiverId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        try {
            TransactionSearchRequest filters = new TransactionSearchRequest();
            filters.setSenderBranchId(senderBranchId);
            filters.setReceiverBranchId(receiverBranchId);
            filters.setSourceCurrency(sourceCurrency);
            filters.setDestinationCurrency(destinationCurrency);
            filters.setMinAmount(minAmount);
            filters.setMaxAmount(maxAmount);
            filters.setSenderId(senderId);
            filters.setReceiverId(receiverId);
            filters.setPage(page);
            filters.setSize(size);
            filters.setSortBy(sortBy);
            filters.setSortDirection(sortDirection);
            
            // Parse status if provided
            if (status != null) {
                try {
                    filters.setStatus(com.mycompany.transfersystem.entity.enums.TransactionStatus.valueOf(status.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            }
            
            Page<Transaction> transactions = auditService.searchTransactions(filters);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get fee modification history
     * GET /api/audit/fees/history
     * Access: SUPER_ADMIN and AUDITOR role
     */
    @GetMapping("/fees/history")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<Map<String, Object>>> getFeeModificationHistory() {
        try {
            List<Map<String, Object>> feeHistory = auditService.getFeeModificationHistory();
            return ResponseEntity.ok(feeHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(List.of(Map.of("error", e.getMessage())));
        }
    }

    /**
     * Get platform fund summary
     * GET /api/audit/platform-summary
     * Access: SUPER_ADMIN only
     */
    @GetMapping("/platform-summary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPlatformSummary() {
        try {
            Map<String, Object> summary = auditService.getPlatformSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get branch transaction report
     * GET /api/audit/branches/{branchId}/transactions
     * Access: BRANCH_MANAGER (must match branchId) or SUPER_ADMIN
     */
    @GetMapping("/branches/{branchId}/transactions")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<Map<String, Object>> getBranchTransactionReport(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "30") Integer days) {
        try {
            Map<String, Object> report = auditService.getBranchTransactionReport(branchId, days);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get commission rates for a branch
     * GET /api/audit/branches/{branchId}/commission-rates
     * Access: BRANCH_MANAGER (must match branchId) or SUPER_ADMIN
     */
    @GetMapping("/branches/{branchId}/commission-rates")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<List<CommissionRate>> getBranchCommissionRates(@PathVariable Long branchId) {
        try {
            List<CommissionRate> commissionRates = auditService.getBranchCommissionRates(branchId);
            return ResponseEntity.ok(commissionRates);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get audit logs
     * GET /api/audit/logs
     * Access: SUPER_ADMIN and AUDITOR role
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {
        try {
            // This would typically use a more sophisticated search
            // For now, we'll return a simple implementation
            List<AuditLog> auditLogs = auditService.getFeeModificationHistory().stream()
                    .map(logMap -> {
                        // Convert back to AuditLog - this is a simplified approach
                        AuditLog auditLog = new AuditLog();
                        auditLog.setId((Long) logMap.get("id"));
                        auditLog.setAction((String) logMap.get("action"));
                        auditLog.setEntity((String) logMap.get("entityType"));
                        auditLog.setEntityId((Long) logMap.get("entityId"));
                        // createdAt is automatically set by @CreationTimestamp
                        return auditLog;
                    })
                    .toList();
            
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}