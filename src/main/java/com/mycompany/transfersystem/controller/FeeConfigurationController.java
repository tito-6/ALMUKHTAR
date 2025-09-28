package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.FeeUpdateRequest;
import com.mycompany.transfersystem.entity.CommissionRate;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.CommissionScope;
import com.mycompany.transfersystem.service.FeeManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/fees")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
public class FeeConfigurationController {

    private final FeeManagementService feeManagementService;

    @Autowired
    public FeeConfigurationController(FeeManagementService feeManagementService) {
        this.feeManagementService = feeManagementService;
    }

    /**
     * Updates a commission rate for a specific branch and scope
     * PUT /api/admin/fees/{branchId}/{scope}
     */
    @PutMapping("/{branchId}/{scope}")
    public ResponseEntity<CommissionRate> updateCommissionRate(
            @PathVariable Long branchId,
            @PathVariable CommissionScope scope,
            @Valid @RequestBody FeeUpdateRequest request,
            Authentication authentication) {
        
        // Handle both real User objects and mock authentication
        User user;
        if (authentication.getPrincipal() instanceof User) {
            user = (User) authentication.getPrincipal();
        } else {
            // For test environment, create a mock user
            user = new User();
            user.setUsername(authentication.getName());
            
            // Safely get the role from authorities
            if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
                String roleString = authentication.getAuthorities().iterator().next().getAuthority();
                if (roleString.startsWith("ROLE_")) {
                    roleString = roleString.replace("ROLE_", "");
                }
                try {
                    user.setRole(com.mycompany.transfersystem.entity.enums.UserRole.valueOf(roleString));
                } catch (IllegalArgumentException e) {
                    // Default to BRANCH_MANAGER if role parsing fails
                    user.setRole(com.mycompany.transfersystem.entity.enums.UserRole.BRANCH_MANAGER);
                }
            } else {
                // Default role if no authorities
                user.setRole(com.mycompany.transfersystem.entity.enums.UserRole.BRANCH_MANAGER);
            }
        }
        
        BigDecimal newRate = request.getNewRate();
        
        CommissionRate updatedRate = feeManagementService.updateCommissionRate(user, branchId, scope, newRate);
        
        return ResponseEntity.ok(updatedRate);
    }

    /**
     * Gets the current commission rate for a specific branch and scope
     * GET /api/admin/fees/{branchId}/{scope}
     */
    @GetMapping("/{branchId}/{scope}")
    public ResponseEntity<CommissionRate> getCommissionRate(
            @PathVariable Long branchId,
            @PathVariable CommissionScope scope) {
        
        CommissionRate rate = feeManagementService.getCommissionRate(branchId, scope);
        return ResponseEntity.ok(rate);
    }

    /**
     * Gets all commission rates for a specific branch
     * GET /api/admin/fees/{branchId}
     */
    @GetMapping("/{branchId}")
    public ResponseEntity<List<CommissionRate>> getCommissionRatesForBranch(
            @PathVariable Long branchId) {
        
        List<CommissionRate> rates = feeManagementService.getCommissionRatesForBranch(branchId);
        return ResponseEntity.ok(rates);
    }
}
