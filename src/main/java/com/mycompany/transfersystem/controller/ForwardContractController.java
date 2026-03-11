package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.ForwardContract;
import com.mycompany.transfersystem.repository.ForwardContractRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fx/forward")
@CrossOrigin(origins = "*")
public class ForwardContractController {

    private final ForwardContractRepository forwardContractRepository;
    private final UserRepository userRepository;

    public ForwardContractController(ForwardContractRepository forwardContractRepository, UserRepository userRepository) {
        this.forwardContractRepository = forwardContractRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-contracts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ForwardContract>> myContracts(@AuthenticationPrincipal UserDetails userDetails) {
        var user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(forwardContractRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()));
    }
}
