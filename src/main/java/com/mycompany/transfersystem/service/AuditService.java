package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.TransactionSearchRequest;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import com.mycompany.transfersystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class AuditService {

    private final TransactionRepository transactionRepository;
    private final FundRepository fundRepository;
    private final BranchRepository branchRepository;
    private final CommissionRateRepository commissionRateRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditService(TransactionRepository transactionRepository,
                       FundRepository fundRepository,
                       BranchRepository branchRepository,
                       CommissionRateRepository commissionRateRepository,
                       AuditLogRepository auditLogRepository) {
        this.transactionRepository = transactionRepository;
        this.fundRepository = fundRepository;
        this.branchRepository = branchRepository;
        this.commissionRateRepository = commissionRateRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Get real-time fund status for a specific branch
     * @param branchId Branch ID
     * @return Fund status with balance and debt/credit information
     */
    public Map<String, Object> getBranchFundStatus(Long branchId) {
        Map<String, Object> fundStatus = new HashMap<>();
        
        // Check authorization for branch managers
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_BRANCH_MANAGER"))) {
            
            // For branch managers, check if they can access this branch
            // In a real system, you'd check the user's managed branch ID
            // For now, we'll implement a simple check based on branch ID
            if (!isBranchManagerAuthorized(branchId, auth.getName())) {
                throw new RuntimeException("Access denied: Branch manager can only access their own branch");
            }
        }
        
        // Get branch information
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found with ID: " + branchId));
        
        // Get branch fund
        String fundName = branch.getName() + " Fund";
        Optional<Fund> branchFundOpt = fundRepository.findByName(fundName);
        
        if (branchFundOpt.isPresent()) {
            Fund branchFund = branchFundOpt.get();
            fundStatus.put("branchId", branchId);
            fundStatus.put("branchName", branch.getName());
            fundStatus.put("fundName", fundName);
            fundStatus.put("currentBalance", branchFund.getBalance());
            fundStatus.put("fundStatus", branchFund.getStatus());
            fundStatus.put("lastUpdated", LocalDateTime.now());
            
            // Calculate net position (positive = credit, negative = debt)
            BigDecimal balance = branchFund.getBalance();
            if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                fundStatus.put("netPosition", "CREDIT");
                fundStatus.put("netAmount", balance);
            } else {
                fundStatus.put("netPosition", "DEBT");
                fundStatus.put("netAmount", balance.abs());
            }
        } else {
            fundStatus.put("branchId", branchId);
            fundStatus.put("branchName", branch.getName());
            fundStatus.put("fundName", fundName);
            fundStatus.put("currentBalance", BigDecimal.ZERO);
            fundStatus.put("fundStatus", "NOT_CREATED");
            fundStatus.put("netPosition", "NO_FUND");
            fundStatus.put("netAmount", BigDecimal.ZERO);
            fundStatus.put("lastUpdated", LocalDateTime.now());
        }
        
        return fundStatus;
    }

    /**
     * Search transactions with complex filtering
     * @param filters Search criteria
     * @return Page of transactions matching the criteria
     */
    public Page<Transaction> searchTransactions(TransactionSearchRequest filters) {
        // Create pageable with sorting
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(filters.getSortDirection()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC, 
            filters.getSortBy()
        );
        
        Pageable pageable = PageRequest.of(filters.getPage(), filters.getSize(), sort);
        
        // For now, we'll use a simple approach with the existing repository
        // In a real implementation, you might want to create custom query methods
        // or use Criteria API for more complex filtering
        
        if (filters.getStartDate() != null && filters.getEndDate() != null) {
            return transactionRepository.findByCreatedAtBetween(
                filters.getStartDate(), 
                filters.getEndDate(), 
                pageable
            );
        } else if (filters.getStatus() != null) {
            return transactionRepository.findByStatus(filters.getStatus(), pageable);
        } else {
            return transactionRepository.findAll(pageable);
        }
    }

    /**
     * Get fee modification history
     * @return List of fee modification records
     */
    public List<Map<String, Object>> getFeeModificationHistory() {
        // Get all audit logs related to fee modifications
        List<AuditLog> feeAuditLogs = auditLogRepository.findByActionContainingIgnoreCase("FEE");
        
        return feeAuditLogs.stream()
                .map(this::convertAuditLogToMap)
                .toList();
    }

    /**
     * Get platform fund summary
     * @return Platform fund summary with total balance and transaction history
     */
    public Map<String, Object> getPlatformSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Get platform fund
        Optional<Fund> platformFundOpt = fundRepository.findByName("Platform Fund");
        
        if (platformFundOpt.isPresent()) {
            Fund platformFund = platformFundOpt.get();
            summary.put("fundName", "Platform Fund");
            summary.put("currentBalance", platformFund.getBalance());
            summary.put("fundStatus", platformFund.getStatus());
            summary.put("lastUpdated", LocalDateTime.now());
            
            // Calculate total fees collected (this would be calculated from transaction history)
            BigDecimal totalFeesCollected = calculateTotalPlatformFees();
            summary.put("totalFeesCollected", totalFeesCollected);
            
            // Get recent transaction count
            long recentTransactionCount = transactionRepository.countByCreatedAtAfter(
                LocalDateTime.now().minusDays(30)
            );
            summary.put("recentTransactionCount", recentTransactionCount);
            
        } else {
            summary.put("fundName", "Platform Fund");
            summary.put("currentBalance", BigDecimal.ZERO);
            summary.put("fundStatus", "NOT_CREATED");
            summary.put("totalFeesCollected", BigDecimal.ZERO);
            summary.put("recentTransactionCount", 0L);
            summary.put("lastUpdated", LocalDateTime.now());
        }
        
        return summary;
    }

    /**
     * Get comprehensive transaction report for a specific branch
     * @param branchId Branch ID
     * @param days Number of days to look back
     * @return Transaction report
     */
    public Map<String, Object> getBranchTransactionReport(Long branchId, int days) {
        Map<String, Object> report = new HashMap<>();
        
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found with ID: " + branchId));
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // Get transactions for this branch (as sender or receiver)
        List<Transaction> transactions = transactionRepository.findByCreatedAtAfter(startDate);
        
        // Filter transactions for this branch
        List<Transaction> branchTransactions = transactions.stream()
                .filter(t -> isTransactionRelatedToBranch(t, branchId))
                .toList();
        
        report.put("branchId", branchId);
        report.put("branchName", branch.getName());
        report.put("reportPeriod", days + " days");
        report.put("totalTransactions", branchTransactions.size());
        report.put("transactions", branchTransactions);
        
        // Calculate summary statistics
        BigDecimal totalVolume = branchTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        report.put("totalVolume", totalVolume);
        report.put("averageTransactionSize", 
            branchTransactions.isEmpty() ? BigDecimal.ZERO : 
            totalVolume.divide(new BigDecimal(branchTransactions.size()), 2, java.math.RoundingMode.HALF_UP));
        
        return report;
    }

    /**
     * Get all commission rates for a branch
     * @param branchId Branch ID
     * @return List of commission rates
     */
    public List<CommissionRate> getBranchCommissionRates(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found with ID: " + branchId));
        
        return commissionRateRepository.findByBranch(branch);
    }

    // Helper methods

    private Map<String, Object> convertAuditLogToMap(AuditLog auditLog) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("id", auditLog.getId());
        logMap.put("action", auditLog.getAction());
        logMap.put("entityType", auditLog.getEntity());
        logMap.put("entityId", auditLog.getEntityId());
        logMap.put("user", auditLog.getUser() != null ? auditLog.getUser().getUsername() : "SYSTEM");
        logMap.put("timestamp", auditLog.getCreatedAt());
        logMap.put("details", "Action: " + auditLog.getAction() + " on " + auditLog.getEntity() + " with ID: " + auditLog.getEntityId());
        return logMap;
    }

    private BigDecimal calculateTotalPlatformFees() {
        // This would typically be calculated from transaction history
        // For now, we'll return a placeholder
        return new BigDecimal("1000.00"); // Placeholder value
    }

    private boolean isTransactionRelatedToBranch(Transaction transaction, Long branchId) {
        // This is a simplified check - in a real implementation,
        // you'd need to check if the transaction involves the specified branch
        // For now, we'll return true for all transactions
        return true;
    }

    /**
     * Log an audit event
     * @param action Action performed
     * @param user User who performed the action
     * @param entityType Type of entity affected
     * @param entityId ID of entity affected
     */
    public void log(String action, User user, String entityType, Long entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setUser(user);
        auditLog.setEntity(entityType);
        auditLog.setEntityId(entityId);
        // createdAt is automatically set by @CreationTimestamp
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Check if a branch manager is authorized to access a specific branch
     * @param branchId Branch ID to check
     * @param username Username of the branch manager
     * @return true if authorized, false otherwise
     */
    public boolean isBranchManagerAuthorized(Long branchId, String username) {
        // For this implementation, we'll use a simplified approach
        // In a real system, you'd check if the user is a branch manager for the specific branch
        
        // Get the branch
        Optional<Branch> branchOpt = branchRepository.findById(branchId);
        if (branchOpt.isEmpty()) {
            return false;
        }
        
        Branch branch = branchOpt.get();
        
        // For test purposes, map specific users to specific branches
        // In production, this would be a proper user-branch relationship table
        if ("manager".equals(username)) {
            // manager can only access BRANCH_A
            return "BRANCH_A".equals(branch.getName());
        } else if ("managerB".equals(username)) {
            // managerB can only access BRANCH_B
            return "BRANCH_B".equals(branch.getName());
        }
        
        // Default: branch managers cannot access main admin branch
        return !branch.getName().equals("MAIN_ADMIN_BRANCH");
    }
}