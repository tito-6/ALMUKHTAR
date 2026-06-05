package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.dispute.OpenDisputeRequest;
import com.mycompany.transfersystem.entity.Dispute;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.dispute.DisputeService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disputes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Dispute> open(@Valid @RequestBody OpenDisputeRequest dto,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(disputeService.openDispute(user.getId(), dto));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Dispute>> myDisputes(@AuthenticationPrincipal UserDetails userDetails, Pageable pageable) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(disputeService.getMyDisputes(user.getId(), pageable));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN','PLATFORM_OWNER')")
    public ResponseEntity<Page<Dispute>> all(Pageable pageable) {
        return ResponseEntity.ok(disputeService.getAllDisputes(pageable));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Void> assign(@PathVariable Long id, @RequestParam Long branchId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        disputeService.assignDispute(id, branchId, user.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Void> resolve(@PathVariable Long id, @RequestParam String resolution,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        disputeService.resolveDispute(id, resolution, user.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Void> reject(@PathVariable Long id, @RequestParam String reason,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        disputeService.rejectDispute(id, reason, user.getId());
        return ResponseEntity.ok().build();
    }
}
