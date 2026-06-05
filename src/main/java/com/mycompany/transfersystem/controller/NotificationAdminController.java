package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.notification.NotificationOutboxResponse;
import com.mycompany.transfersystem.service.notification.NotificationOutboxService;
import com.mycompany.transfersystem.service.notification.NotificationDispatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
public class NotificationAdminController {

    private final NotificationDispatchService notificationDispatchService;
    private final NotificationOutboxService notificationOutboxService;

    public NotificationAdminController(NotificationDispatchService notificationDispatchService,
                                       NotificationOutboxService notificationOutboxService) {
        this.notificationDispatchService = notificationDispatchService;
        this.notificationOutboxService = notificationOutboxService;
    }

    @PostMapping("/delivery/{id}/retry")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Void> retry(@PathVariable("id") Long deliveryLogId) {
        notificationDispatchService.adminRetryDelivery(deliveryLogId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/outbox/dead-letter")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<List<NotificationOutboxResponse>> deadLetters(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(notificationOutboxService.listDeadLetters(limit).stream()
                .map(NotificationOutboxResponse::from)
                .toList());
    }

    @PostMapping("/outbox/{id}/replay")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<NotificationOutboxResponse> replayDeadLetter(@PathVariable Long id) {
        return ResponseEntity.accepted().body(NotificationOutboxResponse.from(notificationOutboxService.replayDeadLetter(id)));
    }
}
