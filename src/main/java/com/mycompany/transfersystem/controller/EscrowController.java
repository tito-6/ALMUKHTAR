package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.EscrowContract;
import com.mycompany.transfersystem.repository.EscrowContractRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.util.SecurityUtils;
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

    public EscrowController(EscrowContractRepository escrowContractRepository, UserRepository userRepository) {
        this.escrowContractRepository = escrowContractRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-contracts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EscrowContract>> myContracts(@AuthenticationPrincipal UserDetails userDetails) {
        var user = SecurityUtils.resolveUser(userDetails, userRepository);
        List<EscrowContract> list = escrowContractRepository.findByInitiatorUser_IdOrBeneficiaryUser_Id(user.getId(), user.getId());
        return ResponseEntity.ok(list);
    }
}
