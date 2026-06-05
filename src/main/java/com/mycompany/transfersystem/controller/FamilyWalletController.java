package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.family.CreateFamilyGroupRequest;
import com.mycompany.transfersystem.entity.FamilyGroup;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.family.FamilyWalletService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/family")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FamilyWalletController {

    private final FamilyWalletService familyWalletService;
    private final UserRepository userRepository;

    @PostMapping("/groups")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<FamilyGroup> createGroup(@Valid @RequestBody CreateFamilyGroupRequest dto,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(familyWalletService.createGroup(user.getId(), dto));
    }

    @PostMapping("/groups/{id}/members")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<Void> addMember(@PathVariable Long id,
                                            @RequestParam Long memberUserId,
                                            @RequestParam(required = false) BigDecimal memberLimit,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        familyWalletService.addMember(id, user.getId(), memberUserId, memberLimit);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/groups/{id}/members/{memberId}")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long memberId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        familyWalletService.removeMember(id, user.getId(), memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups/{id}/statement")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<Map<String, Object>> statement(@PathVariable Long id,
                                                           @RequestParam(required = false) String month,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        YearMonth ym = month != null ? YearMonth.parse(month) : YearMonth.now();
        return ResponseEntity.ok(familyWalletService.getStatement(id, user.getId(), ym));
    }
}
