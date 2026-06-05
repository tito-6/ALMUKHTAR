package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.*;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import com.mycompany.transfersystem.entity.enums.NotificationDeliveryStatus;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.NotificationDeliveryLogRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.notification.provider.MockWhatsAppProvider;
import com.mycompany.transfersystem.service.notification.provider.WhatsAppProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TransferWhatsAppNotificationIT {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationDeliveryLogRepository deliveryLogRepository;

    @Autowired
    private WhatsAppProvider whatsAppProvider;

    private User sender;
    private User receiver;
    private Fund branchAFund;
    private Branch branchA;
    private Branch branchB;

    @BeforeEach
    void setUp() {
        if (whatsAppProvider instanceof MockWhatsAppProvider mock) {
            mock.clear();
        }
        branchA = branchRepository.findFirstByName("BRANCH_A")
                .orElseThrow(() -> new IllegalStateException("BRANCH_A"));
        branchB = branchRepository.findFirstByName("BRANCH_B")
                .orElseThrow(() -> new IllegalStateException("BRANCH_B"));

        long stamp = Instant.now().toEpochMilli();
        sender = new User();
        sender.setUsername("waSender" + stamp);
        sender.setPassword(passwordEncoder.encode("password123"));
        sender.setRole(UserRole.CASHIER);
        sender.setEmail("wa-sender-" + stamp + "@test.com");
        sender.setPhone("+963111111111");
        sender.setBranch(branchA);
        sender = userRepository.save(sender);

        receiver = new User();
        receiver.setUsername("waReceiver" + stamp);
        receiver.setPassword(passwordEncoder.encode("password123"));
        receiver.setRole(UserRole.CASHIER);
        receiver.setEmail("wa-receiver-" + stamp + "@test.com");
        receiver.setPhone("+963222222222");
        receiver.setBranch(branchB);
        receiver = userRepository.save(receiver);

        String fundName = "BRANCH_A Fund";
        branchAFund = fundRepository.findByName(fundName).orElseGet(() -> {
            Fund f = new Fund();
            f.setName(fundName);
            f.setBalance(new BigDecimal("500000.00"));
            f.setStatus(FundStatus.ACTIVE);
            return fundRepository.save(f);
        });
    }

    @Test
    @Commit
    void completedTransfer_emitsSenderAndReceiverWhatsappDeliveryLogs() throws Exception {
        TransferTransactionRequest request = new TransferTransactionRequest();
        request.setSenderId(sender.getId());
        request.setReceiverId(receiver.getId());
        request.setFundId(branchAFund.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setSourceCurrency("USD");
        request.setDestinationCurrency("EUR");
        request.setSenderBranchId(branchA.getId());
        request.setReceiverBranchId(branchB.getId());

        transactionService.executeTransfer(request);

        int matched = 0;
        for (int i = 0; i < 40; i++) {
            List<com.mycompany.transfersystem.entity.NotificationDeliveryLog> all = deliveryLogRepository.findAll();
            matched = (int) all.stream()
                    .filter(l -> sender.getId().equals(l.getUserId()) || receiver.getId().equals(l.getUserId()))
                    .filter(l -> "transfer_sender_completed".equals(l.getTemplateKey())
                            || "transfer_receiver_pickup_ready".equals(l.getTemplateKey()))
                    .filter(l -> l.getStatus() != NotificationDeliveryStatus.SKIPPED)
                    .count();
            if (matched >= 2) {
                break;
            }
            Thread.sleep(250);
        }

        assertThat(matched).isGreaterThanOrEqualTo(2);
    }
}
