package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.*;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import com.mycompany.transfersystem.entity.enums.TransactionStatus;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.notification.provider.WhatsAppProvider;
import com.mycompany.transfersystem.service.notification.provider.WhatsAppSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class TransferWhatsappDispatchNonBlockingIT {

    @MockBean
    private WhatsAppProvider whatsAppProvider;

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

    private User sender;
    private User receiver;
    private Fund branchAFund;
    private Branch branchA;
    private Branch branchB;

    @BeforeEach
    void setUp() {
        when(whatsAppProvider.sendText(anyString(), anyString())).thenReturn(WhatsAppSendResult.fail("down", 503, true));
        when(whatsAppProvider.sendTemplate(anyString(), anyString(), anyString(), anyList())).thenReturn(WhatsAppSendResult.fail("down", 503, true));

        branchA = branchRepository.findFirstByName("BRANCH_A").orElseThrow();
        branchB = branchRepository.findFirstByName("BRANCH_B").orElseThrow();
        long stamp = Instant.now().toEpochMilli();
        sender = userRepository.save(buildUser("nbSender" + stamp, "+963333333333", branchA));
        receiver = userRepository.save(buildUser("nbReceiver" + stamp, "+963444444444", branchB));
        String fundName = "BRANCH_A Fund";
        branchAFund = fundRepository.findByName(fundName).orElseGet(() -> {
            Fund f = new Fund();
            f.setName(fundName);
            f.setBalance(new BigDecimal("500000.00"));
            f.setStatus(FundStatus.ACTIVE);
            return fundRepository.save(f);
        });
    }

    private User buildUser(String username, String phone, Branch branch) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("password123"));
        u.setRole(UserRole.CASHIER);
        u.setEmail(username + "@test.com");
        u.setPhone(phone);
        u.setBranch(branch);
        return u;
    }

    @Test
    @Commit
    void executeTransfer_commitsEvenWhenWhatsappProviderFailsAsync() {
        TransferTransactionRequest request = new TransferTransactionRequest();
        request.setSenderId(sender.getId());
        request.setReceiverId(receiver.getId());
        request.setFundId(branchAFund.getId());
        request.setAmount(new BigDecimal("50.00"));
        request.setSourceCurrency("USD");
        request.setDestinationCurrency("EUR");
        request.setSenderBranchId(branchA.getId());
        request.setReceiverBranchId(branchB.getId());

        TransactionRecordDTO result = transactionService.executeTransfer(request);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.READY_FOR_PICKUP);
    }
}
