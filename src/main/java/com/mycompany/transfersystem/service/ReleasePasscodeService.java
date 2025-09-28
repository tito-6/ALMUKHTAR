package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import com.mycompany.transfersystem.exception.InvalidTransactionException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleasePasscodeService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Verify the release passcode and update transaction status to RELEASED
     * @param transactionId The transaction ID
     * @param passcode The passcode provided by the receiver client
     * @param receiverId The receiver client ID
     * @return true if verification successful and transaction released
     */
    @Transactional
    public boolean verifyPasscode(Long transactionId, String passcode, Long receiverId) {
        // Find the transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // Verify the receiver ID matches
        if (!transaction.getReceiver().getId().equals(receiverId)) {
            throw new InvalidTransactionException("Receiver ID does not match transaction receiver");
        }

        // Check if transaction is in COMPLETED status
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new InvalidTransactionException("Transaction is not in COMPLETED status for release");
        }

        // Verify the passcode
        if (transaction.getReleasePasscode() == null || !transaction.getReleasePasscode().equals(passcode)) {
            throw new InvalidTransactionException("Invalid release passcode");
        }

        // Update transaction status to RELEASED
        transaction.setStatus(TransactionStatus.RELEASED);
        transactionRepository.save(transaction);

        // Send confirmation email to sender
        User sender = transaction.getSender();
        String confirmationMessage = String.format(
            "Your money transfer (Transaction ID: %d) has been successfully released to the receiver. " +
            "Amount: %s. Thank you for using our service.",
            transaction.getId(),
            transaction.getAmount()
        );
        
        notificationService.sendEmail(sender, "Money Transfer Released", confirmationMessage);

        return true;
    }
}
