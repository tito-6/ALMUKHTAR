package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.trading.TradingAssistantChatRequest;
import com.mycompany.transfersystem.dto.trading.TradingAssistantResponse;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.trading.TradingAssistantService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trading/assistant")
@CrossOrigin(origins = "*")
public class TradingAssistantRestController {

    private final TradingAssistantService tradingAssistantService;
    private final UserRepository userRepository;

    public TradingAssistantRestController(TradingAssistantService tradingAssistantService,
                                          UserRepository userRepository) {
        this.tradingAssistantService = tradingAssistantService;
        this.userRepository = userRepository;
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TradingAssistantResponse> chat(
            @Valid @RequestBody TradingAssistantChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(tradingAssistantService.assist(user, request.getMessage()));
    }
}
