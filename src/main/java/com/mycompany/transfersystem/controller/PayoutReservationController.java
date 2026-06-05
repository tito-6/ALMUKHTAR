package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.payout.CreatePayoutReservationRequest;
import com.mycompany.transfersystem.entity.PayoutReservation;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.transfer.PayoutReservationService;
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
@RequestMapping("/api/payout-reservations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PayoutReservationController {

    private final PayoutReservationService payoutReservationService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<PayoutReservation> create(@Valid @RequestBody CreatePayoutReservationRequest req,
                                                      @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(payoutReservationService.create(req, u));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<List<PayoutReservation>> mine(@AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(payoutReservationService.listForReceiver(u));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        User u = SecurityUtils.resolveUser(ud, userRepository);
        payoutReservationService.cancel(id, u);
        return ResponseEntity.ok().build();
    }
}
