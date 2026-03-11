package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.PrepaidCard;
import com.mycompany.transfersystem.repository.PrepaidCardRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "*")
public class CardController {

    private final PrepaidCardRepository prepaidCardRepository;
    private final UserRepository userRepository;

    public CardController(PrepaidCardRepository prepaidCardRepository, UserRepository userRepository) {
        this.prepaidCardRepository = prepaidCardRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-cards")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrepaidCard>> myCards(@AuthenticationPrincipal UserDetails userDetails) {
        var user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(prepaidCardRepository.findByUser_Id(user.getId()));
    }
}
