package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.dispute.OpenDisputeRequest;
import com.mycompany.transfersystem.entity.Dispute;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.DisputeRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.dispute.DisputeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock private DisputeRepository disputeRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks private DisputeService disputeService;

    @Test
    void openDispute_setsSlaDeadlinePlus72h() {
        User user = new User(); user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(disputeRepository.save(any())).thenAnswer(i -> {
            Dispute d = (Dispute) i.getArgument(0);
            d.setId(1L);
            return d;
        });

        OpenDisputeRequest req = new OpenDisputeRequest();
        req.setTransactionId(100L); req.setCategory("WRONG_AMOUNT");
        Dispute result = disputeService.openDispute(1L, req);

        assertNotNull(result.getSlaDeadline());
        assertTrue(result.getSlaDeadline().isAfter(LocalDateTime.now().plusHours(71)));
        assertTrue(result.getSlaDeadline().isBefore(LocalDateTime.now().plusHours(73)));
    }

    @Test
    void resolveDispute_beforeDeadline_slaMetTrue() {
        User reporter = new User();
        reporter.setId(3L);
        Dispute dispute = Dispute.builder().id(1L).status(Dispute.DisputeStatus.OPEN)
                .reporterUser(reporter)
                .slaDeadline(LocalDateTime.now().plusHours(24)).build();
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(new User()));

        disputeService.resolveDispute(1L, "Fixed", 2L);
        assertTrue(dispute.getSlaMet());
    }

    @Test
    void resolveDispute_afterDeadline_slaMetFalse() {
        User reporter = new User();
        reporter.setId(3L);
        Dispute dispute = Dispute.builder().id(1L).status(Dispute.DisputeStatus.OPEN)
                .reporterUser(reporter)
                .slaDeadline(LocalDateTime.now().minusHours(1)).build();
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(new User()));

        disputeService.resolveDispute(1L, "Late fix", 2L);
        assertFalse(dispute.getSlaMet());
    }

    @Test
    void checkSlaDeadlines_pastDeadline_marksSlaMet() {
        Dispute overdue = Dispute.builder().id(1L).status(Dispute.DisputeStatus.OPEN)
                .slaDeadline(LocalDateTime.now().minusDays(1)).build();
        when(disputeRepository.findBySlaDeadlineBefore(any())).thenReturn(List.of(overdue));
        when(disputeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        disputeService.checkSlaDeadlines();
        assertFalse(overdue.getSlaMet());
    }
}
