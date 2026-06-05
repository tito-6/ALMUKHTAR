package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.annotation.RequireIdempotencyKey;
import com.mycompany.transfersystem.dto.escrow.EscrowContractResponse;
import com.mycompany.transfersystem.dto.escrow.EscrowDisputeRequest;
import com.mycompany.transfersystem.entity.EscrowContract;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.EscrowContractRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.escrow.EscrowWorkflowService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/escrow")
@CrossOrigin(origins = "*")
public class EscrowController {

    private final EscrowContractRepository escrowContractRepository;
    private final UserRepository userRepository;
    private final EscrowWorkflowService escrowWorkflowService;

    public EscrowController(EscrowContractRepository escrowContractRepository,
                            UserRepository userRepository,
                            EscrowWorkflowService escrowWorkflowService) {
        this.escrowContractRepository = escrowContractRepository;
        this.userRepository = userRepository;
        this.escrowWorkflowService = escrowWorkflowService;
    }

    @GetMapping("/my-contracts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EscrowContractResponse>> myContracts(@AuthenticationPrincipal UserDetails userDetails) {
        var user = SecurityUtils.resolveUser(userDetails, userRepository);
        List<EscrowContract> list = escrowContractRepository.findByInitiatorUser_IdOrBeneficiaryUser_Id(user.getId(), user.getId());
        return ResponseEntity.ok(list.stream().map(EscrowContractResponse::from).toList());
    }

    @PostMapping("/{id}/fund")
    @RequireIdempotencyKey
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EscrowContractResponse> fund(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        User actor = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(EscrowContractResponse.from(escrowWorkflowService.fund(id, actor)));
    }

    @PostMapping("/{id}/release")
    @RequireIdempotencyKey
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EscrowContractResponse> release(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        User actor = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(EscrowContractResponse.from(escrowWorkflowService.release(id, actor)));
    }

    @PostMapping("/{id}/dispute")
    @RequireIdempotencyKey
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EscrowContractResponse> dispute(@PathVariable Long id,
                                                          @Valid @RequestBody EscrowDisputeRequest request,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        User actor = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(EscrowContractResponse.from(escrowWorkflowService.dispute(id, actor, request.getReasonCategory())));
    }
}
