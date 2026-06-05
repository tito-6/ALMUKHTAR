package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.OfflineTransactionRequest;
import com.mycompany.transfersystem.dto.SyncQueueResponse;
import com.mycompany.transfersystem.dto.SyncResultReport;
import com.mycompany.transfersystem.dto.sync.ResolveSyncConflictRequest;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.SyncQueueService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncQueueController {

    private final SyncQueueService syncQueueService;
    private final UserRepository userRepository;

    public SyncQueueController(SyncQueueService syncQueueService, UserRepository userRepository) {
        this.syncQueueService = syncQueueService;
        this.userRepository = userRepository;
    }

    @PostMapping("/queue")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','BRANCH_MANAGER','CASHIER')")
    public ResponseEntity<SyncQueueResponse> queue(
            @Valid @RequestBody OfflineTransactionRequest request,
            @AuthenticationPrincipal UserDetails ud) {
        User user = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(syncQueueService.enqueue(request, user));
    }

    @PostMapping("/process/{deviceId}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','BRANCH_MANAGER','CASHIER')")
    public ResponseEntity<SyncResultReport> process(
            @PathVariable String deviceId,
            @AuthenticationPrincipal UserDetails ud) {
        User user = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(syncQueueService.processPendingForDevice(deviceId, user));
    }

    @PostMapping("/conflicts/{queueId}/resolve")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','PLATFORM_OWNER')")
    public ResponseEntity<Void> resolveConflict(@PathVariable Long queueId,
                                                @Valid @RequestBody ResolveSyncConflictRequest body,
                                                @AuthenticationPrincipal UserDetails ud) {
        User user = SecurityUtils.resolveUser(ud, userRepository);
        syncQueueService.resolveConflict(queueId, user, body);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{deviceId}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','BRANCH_MANAGER','CASHIER')")
    public ResponseEntity<List<SyncQueueResponse>> status(@PathVariable String deviceId) {
        return ResponseEntity.ok(syncQueueService.getStatusForDevice(deviceId));
    }
}
