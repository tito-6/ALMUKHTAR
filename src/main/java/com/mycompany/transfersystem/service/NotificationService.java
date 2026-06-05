package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.notification.NotificationDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationService {

    private static final String OPS_TEMPLATE = "internal.branch.ops";

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationDispatchService notificationDispatchService;

    /**
     * Send internal branch alert to a specific branch
     * This method sends a message to Branch B when a transaction is created
     * @param branchId The ID of the receiving branch
     * @param message The alert message (does NOT contain the release passcode)
     */
    public void sendInternalBranchAlert(Long branchId, String message) {
        Optional<Branch> branch = branchRepository.findById(branchId);
        if (branch.isPresent()) {
            System.out.println("INTERNAL BRANCH ALERT to " + branch.get().getName() + ": " + message);
        }
    }

    /** Branch hotline + all branch managers on file; internal log always. */
    public void notifyBranchOperation(Long branchId, String title, String detail) {
        sendInternalBranchAlert(branchId, title + ": " + detail);
        branchRepository.findById(branchId).ifPresent(branch -> {
            notificationDispatchService.dispatchBranchOperationalWhatsapp(branch.getPhone(), branchId, title, detail);
        });
        for (User mgr : userRepository.findByBranch_IdAndRole(branchId, UserRole.BRANCH_MANAGER)) {
            if (mgr.getPhone() != null && !mgr.getPhone().isBlank()) {
                notificationDispatchService.dispatchFinancialWhatsapp(mgr.getId(), mgr.getPhone(), OPS_TEMPLATE, title, detail);
            }
        }
    }

    public void notifyUsersByRole(UserRole role, String title, String detail) {
        for (User u : userRepository.findByRole(role)) {
            if (u.getPhone() != null && !u.getPhone().isBlank()) {
                notificationDispatchService.dispatchFinancialWhatsapp(u.getId(), u.getPhone(), OPS_TEMPLATE, title, detail);
            }
        }
    }

    public void notifyUserPhones(User user, String title, String detail) {
        if (user == null) {
            return;
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            notificationDispatchService.dispatchFinancialWhatsapp(user.getId(), user.getPhone(), OPS_TEMPLATE, title, detail);
        }
    }

    /**
     * Send email notification to a user
     */
    public void sendEmail(User user, String subject, String message) {
        System.out.println("EMAIL to " + user.getEmail() + " [" + subject + "]: " + message);
    }

    /**
     * Send SMS notification to a user
     */
    public void sendSMS(User user, String message) {
        System.out.println("SMS to " + user.getPhone() + ": " + message);
    }

    /**
     * Generate a secure release passcode
     * @return A 6-digit numeric passcode
     */
    public String generateReleasePasscode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}
