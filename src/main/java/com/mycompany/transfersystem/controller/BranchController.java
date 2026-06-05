package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.BranchRequest;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.service.FeeManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

/**
 * Admin CRUD for branches. Creating a branch also seeds the standard set of
 * commission rates so it is immediately configurable from the fee screen.
 */
@RestController
@RequestMapping("/api/admin/branches")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN')")
public class BranchController {

    private final BranchRepository branchRepository;
    private final FeeManagementService feeManagementService;

    public BranchController(BranchRepository branchRepository,
                           FeeManagementService feeManagementService) {
        this.branchRepository = branchRepository;
        this.feeManagementService = feeManagementService;
    }

    @GetMapping
    public ResponseEntity<List<Branch>> list() {
        return ResponseEntity.ok(branchRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Branch> get(@PathVariable Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + id));
        return ResponseEntity.ok(branch);
    }

    @PostMapping
    public ResponseEntity<Branch> create(@Valid @RequestBody BranchRequest request) {
        Branch branch = new Branch();
        applyRequest(branch, request);
        Branch saved = branchRepository.save(branch);
        // Seed default commission rates so the branch is configurable right away.
        feeManagementService.createDefaultRatesForBranch(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Branch> update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + id));
        applyRequest(branch, request);
        return ResponseEntity.ok(branchRepository.save(branch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!branchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Branch not found with ID: " + id);
        }
        branchRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applyRequest(Branch branch, BranchRequest request) {
        branch.setName(request.getName());
        branch.setCity(request.getCity());
        branch.setCountry(request.getCountry());
        branch.setAddressLine(request.getAddressLine());
        branch.setPhone(request.getPhone());
        branch.setLatitude(request.getLatitude());
        branch.setLongitude(request.getLongitude());
        branch.setOpensAt(parseTime(request.getOpensAt()));
        branch.setClosesAt(parseTime(request.getClosesAt()));
        branch.setServices(request.getServices());
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalTime.parse(value.trim());
    }
}
