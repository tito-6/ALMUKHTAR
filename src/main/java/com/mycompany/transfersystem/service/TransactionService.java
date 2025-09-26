package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.TransactionResponse;
import com.mycompany.transfersystem.dto.TransferRequest;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import com.mycompany.transfersystem.exception.InsufficientFundsException;
import com.mycompany.transfersystem.exception.InvalidTransactionException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.TransactionRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private AuditService auditService;

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return convertToResponse(transaction);
    }

    @Transactional
    public TransactionResponse createTransfer(TransferRequest request) {
        // Validate sender and receiver
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found with id: " + request.getSenderId()));
        
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + request.getReceiverId()));

        // Validate fund
        Fund fund = fundRepository.findById(request.getFundId())
                .orElseThrow(() -> new ResourceNotFoundException("Fund not found with id: " + request.getFundId()));

        // Validate fund status
        if (fund.getStatus() != FundStatus.ACTIVE) {
            throw new InvalidTransactionException("Fund is not active");
        }

        // Validate sender and receiver are different
        if (sender.getId().equals(receiver.getId())) {
            throw new InvalidTransactionException("Sender and receiver cannot be the same");
        }

        // Validate sufficient balance
        if (fund.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance in fund: " + fund.getName());
        }

        // Create transaction with PENDING status
        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setFund(fund);
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.PENDING);

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            // Update fund balance
            fund.setBalance(fund.getBalance().subtract(request.getAmount()));
            fundRepository.save(fund);

            // Update transaction status to COMPLETED
            savedTransaction.setStatus(TransactionStatus.COMPLETED);
            savedTransaction = transactionRepository.save(savedTransaction);

            // Log the transaction
            User currentUser = getCurrentUser();
            auditService.log("CREATE_TRANSACTION", currentUser, "Transaction", savedTransaction.getId());

        } catch (Exception e) {
            // If something goes wrong, mark transaction as FAILED
            savedTransaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(savedTransaction);
            throw new InvalidTransactionException("Transaction failed: " + e.getMessage());
        }

        return convertToResponse(savedTransaction);
    }

    public List<TransactionResponse> getTransactionsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return transactionRepository.findByUser(user).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse convertToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSender().getUsername(),
                transaction.getReceiver().getUsername(),
                transaction.getFund().getName(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}