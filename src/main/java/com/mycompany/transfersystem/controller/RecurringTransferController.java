package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.recurring.CreateRecurringTransferRequest;
import com.mycompany.transfersystem.entity.RecurringTransfer;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.recurring.RecurringTransferService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transfers")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RecurringTransferController {

    private final RecurringTransferService recurringTransferService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<RecurringTransfer> create(@Valid @RequestBody CreateRecurringTransferRequest dto,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(recurringTransferService.create(user.getId(), dto));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<List<RecurringTransfer>> my(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(recurringTransferService.getMyTransfers(user.getId()));
    }

    @PatchMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<Void> pause(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        recurringTransferService.pause(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<Void> resume(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        recurringTransferService.resume(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        recurringTransferService.cancel(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
