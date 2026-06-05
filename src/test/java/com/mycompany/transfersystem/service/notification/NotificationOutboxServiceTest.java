package com.mycompany.transfersystem.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.entity.NotificationOutbox;
import com.mycompany.transfersystem.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxServiceTest {

    @Mock
    NotificationOutboxRepository repository;

    NotificationOutboxService service;

    @BeforeEach
    void setUp() {
        service = new NotificationOutboxService(repository, new ObjectMapper());
    }

    @Test
    void enqueue_existingCorrelationIdDoesNotPersistDuplicate() {
        NotificationCommand cmd = new NotificationCommand(
                "TRANSFER_CREATED",
                "Transaction",
                1L,
                2L,
                null,
                "WHATSAPP",
                "***",
                "+966500000000",
                "transfer_created_sender",
                "ar",
                Map.of("amount", "10"),
                null);

        when(repository.existsByCorrelationId("corr-fixed")).thenReturn(true);
        when(repository.findByCorrelationId("corr-fixed"))
                .thenReturn(Optional.of(NotificationOutbox.builder().id(42L).build()));

        Long id = service.enqueue(cmd, "corr-fixed");

        assertThat(id).isEqualTo(42L);
        verify(repository, never()).save(any());
    }
}
