package com.mycompany.transfersystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.dto.OfflineTransactionRequest;
import com.mycompany.transfersystem.dto.SyncQueueResponse;
import com.mycompany.transfersystem.entity.SyncQueue;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.SyncStatus;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.SyncQueueRepository;
import com.mycompany.transfersystem.util.QrEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncQueueServiceTest {

    @Mock
    private SyncQueueRepository syncQueueRepository;
    @Mock
    private TransactionService transactionService;
    @Mock
    private FundRepository fundRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private QrEncryptionUtil encryptionUtil;
    @Mock
    private com.mycompany.transfersystem.repository.CashierShiftRepository cashierShiftRepository;

    private SyncQueueService service;
    private User cashier;

    @BeforeEach
    void setUp() {
        service = new SyncQueueService(syncQueueRepository, transactionService, fundRepository,
                auditService, encryptionUtil, new ObjectMapper().findAndRegisterModules(), cashierShiftRepository);
        cashier = new User();
        cashier.setId(7L);
        cashier.setUsername("cashier-7");
    }

    @Test
    void enqueueStoresExplicitIdempotencyKey() {
        OfflineTransactionRequest request = request();
        request.setIdempotencyKey("device-1-seq-99");

        when(syncQueueRepository.findByDeviceIdAndIdempotencyKey("device-1", "device-1-seq-99"))
                .thenReturn(Optional.empty());
        when(encryptionUtil.encrypt(any())).thenReturn("encrypted-payload");
        when(syncQueueRepository.save(any())).thenAnswer(invocation -> {
            SyncQueue item = invocation.getArgument(0);
            item.setId(123L);
            return item;
        });

        SyncQueueResponse response = service.enqueue(request, cashier);

        ArgumentCaptor<SyncQueue> captor = ArgumentCaptor.forClass(SyncQueue.class);
        verify(syncQueueRepository).save(captor.capture());
        assertThat(response.getQueueId()).isEqualTo(123L);
        assertThat(response.getStatus()).isEqualTo("QUEUED");
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("device-1-seq-99");
        assertThat(captor.getValue().getPayloadChecksum()).hasSize(64);
    }

    @Test
    void enqueueDuplicateReturnsExistingQueueItemWithoutSavingAgain() {
        OfflineTransactionRequest request = request();
        request.setIdempotencyKey("device-1-seq-100");
        SyncQueue existing = SyncQueue.builder()
                .id(555L)
                .deviceId("device-1")
                .idempotencyKey("device-1-seq-100")
                .status(SyncStatus.PENDING)
                .build();

        when(syncQueueRepository.findByDeviceIdAndIdempotencyKey("device-1", "device-1-seq-100"))
                .thenReturn(Optional.of(existing));

        SyncQueueResponse response = service.enqueue(request, cashier);

        assertThat(response.getQueueId()).isEqualTo(555L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(syncQueueRepository, never()).save(any());
        verify(encryptionUtil, never()).encrypt(any());
    }

    private OfflineTransactionRequest request() {
        OfflineTransactionRequest request = new OfflineTransactionRequest();
        request.setDeviceId("device-1");
        request.setFundId(10L);
        request.setSenderId(11L);
        request.setReceiverId(12L);
        request.setAmount(new BigDecimal("250.00"));
        request.setOfflineTimestamp(Instant.parse("2026-05-13T10:00:00Z"));
        return request;
    }
}
