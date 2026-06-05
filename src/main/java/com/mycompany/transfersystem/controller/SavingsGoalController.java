package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.savings.CreateSavingsGoalRequest;
import com.mycompany.transfersystem.entity.SavingsGoal;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.savings.SavingsGoalService;
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
@RequestMapping("/api/savings-goals")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('INDIVIDUAL_USER')")
    public ResponseEntity<SavingsGoal> create(@Valid @RequestBody CreateSavingsGoalRequest dto,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(savingsGoalService.createGoal(user.getId(), dto));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('INDIVIDUAL_USER')")
    public ResponseEntity<List<SavingsGoal>> myGoals(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(savingsGoalService.getMyGoals(user.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INDIVIDUAL_USER')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        savingsGoalService.cancelGoal(id);
        return ResponseEntity.noContent().build();
    }
}
