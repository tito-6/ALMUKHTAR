package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.split.CreateSplitRequest;
import com.mycompany.transfersystem.entity.SplitRequest;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.split.SplitPaymentService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/split")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SplitPaymentController {

    private final SplitPaymentService splitPaymentService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<SplitRequest> create(@Valid @RequestBody CreateSplitRequest dto,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(splitPaymentService.createSplit(user.getId(), dto));
    }

    @PostMapping("/{participantId}/respond")
    @PreAuthorize("hasRole('INDIVIDUAL_USER')")
    public ResponseEntity<Void> respond(@PathVariable Long participantId,
                                         @RequestParam boolean accept,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        splitPaymentService.respondToSplit(participantId, user.getId(), accept);
        return ResponseEntity.ok().build();
    }
}
