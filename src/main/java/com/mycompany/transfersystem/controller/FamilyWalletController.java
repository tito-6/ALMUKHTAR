package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.family.AddFamilyMemberRequest;
import com.mycompany.transfersystem.dto.family.CreateFamilyGroupRequest;
import com.mycompany.transfersystem.entity.FamilyGroup;
import com.mycompany.transfersystem.entity.FamilyMember;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.FamilyGroupRepository;
import com.mycompany.transfersystem.repository.FamilyMemberRepository;
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

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/family")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FamilyWalletController {

    private final FamilyWalletService familyWalletService;
    private final FamilyGroupRepository familyGroupRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;

    @GetMapping("/groups/my")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<List<FamilyGroup>> myGroups(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(familyGroupRepository.findByOwnerUser_Id(user.getId()));
    }

    @PostMapping("/groups")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<FamilyGroup> createGroup(@Valid @RequestBody CreateFamilyGroupRequest dto,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(familyWalletService.createGroup(user.getId(), dto));
    }

    @GetMapping("/groups/{id}/members")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<List<FamilyMember>> getMembers(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        List<FamilyMember> members = familyMemberRepository.findByFamilyGroupId(id);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/groups/{id}/members")
    @PreAuthorize("hasAnyRole('INDIVIDUAL_USER','CORPORATE_ADMIN')")
    public ResponseEntity<Void> addMember(@PathVariable Long id,
                                            @Valid @RequestBody AddFamilyMemberRequest dto,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        familyWalletService.addMember(id, user.getId(), dto.getMemberUserId(), dto.getMemberLimit());
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
