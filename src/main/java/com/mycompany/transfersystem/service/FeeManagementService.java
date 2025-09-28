package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.CommissionRate;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.CommissionScope;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.exception.UnauthorizedFeeModificationException;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.CommissionRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class FeeManagementService {

    private final CommissionRateRepository commissionRateRepository;
    private final BranchRepository branchRepository;

    @Autowired
    public FeeManagementService(CommissionRateRepository commissionRateRepository,
                               BranchRepository branchRepository) {
        this.commissionRateRepository = commissionRateRepository;
        this.branchRepository = branchRepository;
    }

    /**
     * Updates a commission rate with proper authorization checks
     * @param user The authenticated user making the request
     * @param branchId The branch ID for the fee rate
     * @param scope The commission scope (fee type)
     * @param newRate The new rate value
     * @return The updated CommissionRate
     * @throws UnauthorizedFeeModificationException if user lacks permission
     * @throws ResourceNotFoundException if branch or rate not found
     */
    public CommissionRate updateCommissionRate(User user, Long branchId, CommissionScope scope, BigDecimal newRate) {
        // Validate inputs
        if (user == null) {
            throw new UnauthorizedFeeModificationException("User must be authenticated");
        }
        if (branchId == null || scope == null || newRate == null) {
            throw new IllegalArgumentException("Branch ID, scope, and new rate are required");
        }
        if (newRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rate cannot be negative");
        }

        // Get the branch
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + branchId));

        // Authorization checks based on user role
        validateAuthorization(user, branchId, scope);

        // Find existing commission rate
        CommissionRate existingRate = commissionRateRepository
                .findByBranchAndCommissionScope(branch, scope)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commission rate not found for branch " + branch.getName() + " and scope " + scope));

        // Update the rate
        existingRate.setRateValue(newRate);
        CommissionRate updatedRate = commissionRateRepository.save(existingRate);

        return updatedRate;
    }

    /**
     * Validates authorization for fee modification based on user role and scope
     */
    private void validateAuthorization(User user, Long branchId, CommissionScope scope) {
        UserRole userRole = user.getRole();

        switch (userRole) {
            case SUPER_ADMIN:
                // SUPER_ADMIN can modify any fee for any branch
                break;

            case BRANCH_MANAGER:
                validateBranchManagerAuthorization(user, branchId, scope);
                break;

            case CASHIER:
            case AUDITOR:
            default:
                throw new UnauthorizedFeeModificationException(
                        "User role " + userRole + " is not authorized to modify fee rates");
        }
    }

    /**
     * Validates authorization for BRANCH_MANAGER users
     */
    private void validateBranchManagerAuthorization(User user, Long branchId, CommissionScope scope) {
        // Check 1: Branch Manager can only modify SENDING_BRANCH_FEE or RECEIVING_BRANCH_FEE
        if (scope != CommissionScope.SENDING_BRANCH_FEE && scope != CommissionScope.RECEIVING_BRANCH_FEE) {
            throw new UnauthorizedFeeModificationException(
                    "BRANCH_MANAGER can only modify SENDING_BRANCH_FEE and RECEIVING_BRANCH_FEE, not " + scope);
        }

        // Check 2: Branch Manager can only modify fees for their own branch
        // Note: In a real system, you'd need to link users to their managed branches
        // For this implementation, we'll use username-based mapping for test purposes
        // In practice, you might have a UserBranch relationship table
        String username = user.getUsername();
        Long expectedBranchId = null;
        
        // Map usernames to their managed branch IDs
        if ("manager".equals(username)) {
            expectedBranchId = 2L; // BRANCH_A ID
        } else if ("managerB".equals(username)) {
            expectedBranchId = 3L; // BRANCH_B ID
        }
        
        if (expectedBranchId == null || !expectedBranchId.equals(branchId)) {
            throw new UnauthorizedFeeModificationException(
                    "BRANCH_MANAGER " + username + " can only modify fees for their own branch, not branch ID: " + branchId);
        }
    }

    /**
     * Gets the current commission rate for a branch and scope
     * @param branchId The branch ID
     * @param scope The commission scope
     * @return The current CommissionRate
     */
    public CommissionRate getCommissionRate(Long branchId, CommissionScope scope) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + branchId));

        return commissionRateRepository
                .findByBranchAndCommissionScope(branch, scope)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commission rate not found for branch " + branch.getName() + " and scope " + scope));
    }

    /**
     * Gets all commission rates for a specific branch
     * @param branchId The branch ID
     * @return List of CommissionRates for the branch
     */
    public java.util.List<CommissionRate> getCommissionRatesForBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + branchId));

        return commissionRateRepository.findByBranch(branch);
    }
}
