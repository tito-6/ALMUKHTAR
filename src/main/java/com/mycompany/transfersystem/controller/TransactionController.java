package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.*;
import com.mycompany.transfersystem.service.TransactionService;
import com.mycompany.transfersystem.service.ReleasePasscodeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ReleasePasscodeService releasePasscodeService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('AUDITOR')")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        List<TransactionResponse> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long id) {
        TransactionResponse transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('CASHIER')")
    public ResponseEntity<TransactionResponse> createTransfer(@Valid @RequestBody TransferRequest request) {
        TransactionResponse transaction = transactionService.createTransfer(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByUser(@PathVariable Long userId) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByUser(userId);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/transfer-comprehensive")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('CASHIER')")
    public ResponseEntity<TransactionRecordDTO> executeTransfer(@Valid @RequestBody TransferTransactionRequest request) {
        TransactionRecordDTO transaction = transactionService.executeTransfer(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/record")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('CASHIER')")
    public ResponseEntity<TransactionRecordDTO> getTransactionRecord(@PathVariable Long id) {
        // In a real implementation, you would get the current user's branch ID from security context
        // For now, we'll pass null to show the passcode (admin view)
        TransactionRecordDTO record = transactionService.getTransactionRecordById(id, null);
        return ResponseEntity.ok(record);
    }

    @PostMapping("/{transactionId}/release")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('CASHIER')")
    public ResponseEntity<String> releaseTransaction(@PathVariable Long transactionId, 
                                                   @Valid @RequestBody ReleaseRequest request) {
        try {
            boolean released = releasePasscodeService.verifyPasscode(transactionId, request.getPasscode(), request.getReceiverId());
            if (released) {
                return ResponseEntity.ok("Transaction released successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to release transaction");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}