package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.freeze.CreateAccountFreezeCaseRequest;
import com.mycompany.transfersystem.dto.freeze.PublicFreezeCaseView;
import com.mycompany.transfersystem.entity.AccountFreezeCase;
import com.mycompany.transfersystem.entity.FreezeCaseEvent;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.compliance.AccountFreezeService;
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
@RequestMapping("/api/compliance/freeze-cases")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AccountFreezeController {

    private final AccountFreezeService accountFreezeService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('MOTHER_BRANCH_ADMIN','PLATFORM_OWNER','AUDITOR')")
    public ResponseEntity<AccountFreezeCase> create(@Valid @RequestBody CreateAccountFreezeCaseRequest req,
                                                    @AuthenticationPrincipal UserDetails ud) {
        User actor = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(accountFreezeService.openCase(req, actor));
    }

    @GetMapping("/public/{publicCaseId}")
    public ResponseEntity<PublicFreezeCaseView> publicView(@PathVariable String publicCaseId) {
        return ResponseEntity.ok(accountFreezeService.getPublicView(publicCaseId));
    }

    @GetMapping("/{caseId}/events")
    @PreAuthorize("hasAnyRole('MOTHER_BRANCH_ADMIN','PLATFORM_OWNER','AUDITOR')")
    public ResponseEntity<List<FreezeCaseEvent>> events(@PathVariable Long caseId,
                                                          @AuthenticationPrincipal UserDetails ud) {
        User actor = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(accountFreezeService.listEventsForCase(caseId, actor));
    }

    @PostMapping("/{caseId}/release")
    @PreAuthorize("hasAnyRole('MOTHER_BRANCH_ADMIN','PLATFORM_OWNER')")
    public ResponseEntity<AccountFreezeCase> release(@PathVariable Long caseId,
                                                     @RequestParam(required = false) String internalNote,
                                                     @AuthenticationPrincipal UserDetails ud) {
        User actor = SecurityUtils.resolveUser(ud, userRepository);
        return ResponseEntity.ok(accountFreezeService.releaseCase(caseId, actor, internalNote));
    }
}
