package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.FundRequest;
import com.mycompany.transfersystem.dto.FundResponse;
import com.mycompany.transfersystem.service.FundService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funds")
@CrossOrigin(origins = "*")
public class FundController {

    @Autowired
    private FundService fundService;

    @GetMapping
    public ResponseEntity<List<FundResponse>> getAllFunds() {
        List<FundResponse> funds = fundService.getAllFunds();
        return ResponseEntity.ok(funds);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FundResponse> getFundById(@PathVariable Long id) {
        FundResponse fund = fundService.getFundById(id);
        return ResponseEntity.ok(fund);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<FundResponse> createFund(@Valid @RequestBody FundRequest request) {
        FundResponse createdFund = fundService.createFund(request);
        return new ResponseEntity<>(createdFund, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER')")
    public ResponseEntity<FundResponse> updateFund(@PathVariable Long id, 
                                                 @Valid @RequestBody FundRequest request) {
        FundResponse updatedFund = fundService.updateFund(id, request);
        return ResponseEntity.ok(updatedFund);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteFund(@PathVariable Long id) {
        fundService.deleteFund(id);
        return ResponseEntity.noContent().build();
    }
}