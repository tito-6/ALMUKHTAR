package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.config.properties.AlmukhtarNotificationOutboxProperties;
import com.mycompany.transfersystem.entity.NotificationOutbox;
import com.mycompany.transfersystem.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxWorkerTest {

    @Mock
    AlmukhtarNotificationOutboxProperties properties;
    @Mock
    NotificationOutboxRepository repository;
    @Mock
    NotificationOutboxProcessor processor;

    @InjectMocks
    NotificationOutboxWorker worker;

    @Test
    void processBatch_callsProcessorForEachDueRow() {
        when(properties.getBatchSize()).thenReturn(50);
        when(properties.getMaxAttempts()).thenReturn(5);
        when(properties.getInitialBackoffSeconds()).thenReturn(30L);

        NotificationOutbox r1 = mock(NotificationOutbox.class);
        when(r1.getId()).thenReturn(10L);
        NotificationOutbox r2 = mock(NotificationOutbox.class);
        when(r2.getId()).thenReturn(20L);
        when(repository.findDueBatch(anyList(), any(Instant.class), any(Pageable.class))).thenReturn(List.of(r1, r2));

        worker.processBatch();

        verify(processor).processRow(10L, 5, 30L);
        verify(processor).processRow(20L, 5, 30L);
    }

    @Test
    void processBatch_oneProcessorFailureStillInvokesFollowingRows() {
        when(properties.getBatchSize()).thenReturn(50);
        when(properties.getMaxAttempts()).thenReturn(5);
        when(properties.getInitialBackoffSeconds()).thenReturn(30L);

        NotificationOutbox r1 = mock(NotificationOutbox.class);
        when(r1.getId()).thenReturn(1L);
        NotificationOutbox r2 = mock(NotificationOutbox.class);
        when(r2.getId()).thenReturn(2L);
        when(repository.findDueBatch(anyList(), any(Instant.class), any(Pageable.class))).thenReturn(List.of(r1, r2));

        doThrow(new RuntimeException("prov")).when(processor).processRow(eq(1L), eq(5), eq(30L));

        worker.processBatch();

        verify(processor).processRow(2L, 5, 30L);
    }

    @Test
    void tick_swallowsRepositoryFailures() {
        when(properties.isEnabled()).thenReturn(true);
        when(repository.findDueBatch(anyList(), any(Instant.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("db"));

        worker.tick();

        verify(repository, times(1)).findDueBatch(anyList(), any(Instant.class), any(Pageable.class));
    }
}
