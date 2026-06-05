package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.ai.AiHelperRequest;
import com.mycompany.transfersystem.dto.ai.AiHelperResponse;
import com.mycompany.transfersystem.dto.ai.AiSessionDetailResponse;
import com.mycompany.transfersystem.dto.ai.AiSessionSummaryResponse;
import com.mycompany.transfersystem.dto.ai.AiToolConfirmationRequest;
import com.mycompany.transfersystem.entity.AiConversationMessage;
import com.mycompany.transfersystem.entity.AiConversationSession;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.AiConversationMessageRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.ai.AiHelperOrchestratorService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/helper")
@RequiredArgsConstructor
public class AiHelperController {

    private final AiHelperOrchestratorService orchestratorService;
    private final UserRepository userRepository;
    private final AiConversationMessageRepository messageRepository;

    @PostMapping("/message")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiHelperResponse> message(
            @Valid @RequestBody AiHelperRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(orchestratorService.handleMessage(user, request));
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AiSessionSummaryResponse>> sessions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        List<AiSessionSummaryResponse> list = orchestratorService.listSessions(user).stream()
                .map(s -> AiSessionSummaryResponse.builder()
                        .id(s.getId())
                        .agentRole(s.getAgentRole())
                        .createdAt(s.getCreatedAt())
                        .externalConversationId(s.getExternalConversationId())
                        .build())
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiSessionDetailResponse> session(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        AiConversationSession session = orchestratorService.getSession(user, id);
        List<AiSessionDetailResponse.MessageLine> lines = messageRepository.findBySession_IdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(m -> AiSessionDetailResponse.MessageLine.builder()
                        .direction(m.getDirection())
                        .channel(m.getChannel())
                        .contentMasked(m.getContentMasked())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(AiSessionDetailResponse.builder()
                .id(session.getId())
                .agentRole(session.getAgentRole())
                .externalConversationId(session.getExternalConversationId())
                .createdAt(session.getCreatedAt())
                .messages(lines)
                .build());
    }

    @PostMapping("/tool-confirmation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiHelperResponse> toolConfirmation(
            @Valid @RequestBody AiToolConfirmationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(orchestratorService.confirmTool(user, request));
    }
}
