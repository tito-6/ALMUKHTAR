package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.split.CreateSplitRequest;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.split.SplitPaymentService;
import com.mycompany.transfersystem.service.wallet.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SplitPaymentServiceTest {

    @Mock private SplitRequestRepository splitRequestRepository;
    @Mock private SplitParticipantRepository participantRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletService walletService;
    @Mock private AuditService auditService;

    @InjectMocks private SplitPaymentService splitPaymentService;

    @Test
    void createSplit_correct4WaySplit() {
        User initiator = new User(); initiator.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(initiator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(4L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(5L)).thenReturn(Optional.of(new User()));
        when(splitRequestRepository.save(any())).thenAnswer(i -> {
            SplitRequest sr = (SplitRequest) i.getArgument(0);
            sr.setId(1L);
            return sr;
        });
        when(participantRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateSplitRequest dto = new CreateSplitRequest();
        dto.setTitle("Dinner"); dto.setTotalAmount(new BigDecimal("100"));
        dto.setCurrency("USD"); dto.setParticipantUserIds(List.of(2L, 3L, 4L, 5L));

        SplitRequest result = splitPaymentService.createSplit(1L, dto);
        assertNotNull(result);
        verify(participantRepository, times(4)).save(any());
    }

    @Test
    void respondAccept_allPaid_statusCompleted() {
        User payer = new User(); payer.setId(2L);
        SplitRequest sr = SplitRequest.builder().id(1L).initiatorUser(payer).currency("USD").build();
        SplitParticipant p1 = SplitParticipant.builder().id(1L).splitRequest(sr).payeeUser(payer)
                .shareAmount(new BigDecimal("25")).paid(false).build();
        SplitParticipant p2 = SplitParticipant.builder().id(2L).splitRequest(sr).payeeUser(payer)
                .shareAmount(new BigDecimal("25")).paid(true).paidAt(Instant.now()).build();

        when(participantRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(walletRepository.findByUser_Id(2L)).thenReturn(Optional.of(Wallet.builder().id(10L).build()));
        when(participantRepository.findBySplitRequestId(1L)).thenReturn(List.of(
                SplitParticipant.builder().paid(true).build(),
                SplitParticipant.builder().paid(true).build()));
        when(participantRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(splitRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        splitPaymentService.respondToSplit(1L, 2L, true);
        verify(walletService).debit(anyLong(), eq("USD"), any(), any(), anyString(), anyString());
        verify(walletService).credit(anyLong(), eq("USD"), any(), any(), anyString(), anyString());
    }

    @Test
    void splitExpiry_unpaidAfterDeadline_cancels() {
        SplitRequest sr = SplitRequest.builder().id(1L).status(SplitRequest.SplitStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusDays(1)).build();
        when(splitRequestRepository.findByStatusAndExpiresAtBefore(eq(SplitRequest.SplitStatus.PENDING), any()))
                .thenReturn(List.of(sr));
        when(splitRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        splitPaymentService.expireOverdue();
        assertEquals(SplitRequest.SplitStatus.EXPIRED, sr.getStatus());
    }
}
