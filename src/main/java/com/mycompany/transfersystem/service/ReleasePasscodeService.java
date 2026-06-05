package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.service.transfer.PayoutCompletionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleasePasscodeService {

    @Autowired
    private PayoutCompletionService payoutCompletionService;

    /**
     * Verify the release passcode and complete payout via {@link PayoutCompletionService}.
     *
     * @param actor authenticated user performing the release (typically cashier)
     */
    @Transactional
    public boolean verifyPasscode(Long transactionId, String passcode, Long receiverId, User actor) {
        payoutCompletionService.completeAfterPasscodeRelease(transactionId, passcode, receiverId, actor);
        return true;
    }
}
