package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.AiChatRequest;
import com.mycompany.transfersystem.dto.AiChatResponse;
import com.mycompany.transfersystem.service.ai.AlmukhtarAiService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@ConditionalOnBean(AlmukhtarAiService.class)
public class AiAssistantController {

    private final AlmukhtarAiService aiService;
    private final com.mycompany.transfersystem.repository.UserRepository userRepository;

    public AiAssistantController(AlmukhtarAiService aiService,
                                 com.mycompany.transfersystem.repository.UserRepository userRepository) {
        this.aiService = aiService;
        this.userRepository = userRepository;
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = SecurityUtils.resolveUserId(userDetails, userRepository);
        String conversationId = userDetails.getUsername() + "_" + (request.getSessionId() != null ? request.getSessionId() : "default");
        return ResponseEntity.ok(aiService.chat(request.getMessage(), userId, conversationId));
    }

    @PostMapping("/corporate/forecast")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<AiChatResponse> corporateForecast(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = SecurityUtils.resolveUserId(userDetails, userRepository);
        String enriched = "[CORPORATE_CONTEXT] " + request.getMessage();
        String conversationId = userDetails.getUsername() + "_" + (request.getSessionId() != null ? request.getSessionId() : "corp");
        return ResponseEntity.ok(aiService.chat(enriched, userId, conversationId));
    }
}
