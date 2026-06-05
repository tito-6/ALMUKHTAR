package com.mycompany.transfersystem.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.context.TenantContextHolder;
import com.mycompany.transfersystem.dto.ai.AiHelperRequest;
import com.mycompany.transfersystem.dto.ai.AiHelperResponse;
import com.mycompany.transfersystem.dto.ai.AiToolConfirmationRequest;
import com.mycompany.transfersystem.entity.AiConversationMessage;
import com.mycompany.transfersystem.entity.AiConversationSession;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.AiAgentRole;
import com.mycompany.transfersystem.entity.enums.AiToolName;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.AiConversationSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiHelperOrchestratorService {

    private final AiConversationAuditService auditService;
    private final AiToolRegistry toolRegistry;
    private final ObjectProvider<AlmukhtarAiService> aiServiceProvider;
    private final ObjectMapper objectMapper;
    private final AiConversationSessionRepository sessionRepository;

    @Transactional
    public AiHelperResponse handleMessage(User user, AiHelperRequest request) {
        Long tenantId = TenantContextHolder.getTenantId();
        AiAgentRole role = resolveEffectiveRole(user, request.getAgentRole());
        AiConversationSession session = auditService.findOrCreateSession(user, tenantId, role, request.getSessionKey());

        if (request.getStructuredToolCall() != null) {
            AiHelperRequest.StructuredToolCall tc = request.getStructuredToolCall();
            AiToolName tool = tc.getTool();
            Map<String, Object> args = tc.getArguments() != null ? tc.getArguments() : Collections.emptyMap();
            Map<String, Object> result;
            boolean ok = true;
            try {
                result = toolRegistry.execute(role, user, args, tool);
            } catch (org.springframework.security.access.AccessDeniedException e) {
                throw e;
            } catch (Exception e) {
                ok = false;
                result = Map.of("error", e.getClass().getSimpleName(), "message", String.valueOf(e.getMessage()));
            }
            String argsForAudit = AiDataMaskingUtil.maskPhonesAndIds(String.valueOf(args));
            auditService.recordToolCall(session, user, tenantId, tool, argsForAudit,
                    result != null ? result.toString() : "", ok);

            if (result.containsKey("confirmationToken")
                    && (tool == AiToolName.CREATE_DISPUTE_DRAFT || tool == AiToolName.CREATE_PRICE_ALERT_DRAFT)) {
                auditService.savePendingAction(session, result);
            }

            String userMsg = AiPromptInjectionGuard.sanitizeUserMessage(
                    request.getMessage() != null ? request.getMessage() : "");
            auditService.appendMessage(session, AiConversationMessage.DIRECTION_USER, AiConversationMessage.CHANNEL_WEB,
                    AiDataMaskingUtil.maskPhonesAndIds(userMsg));
            JsonNode toolNode = objectMapper.valueToTree(result);
            auditService.appendMessage(session, AiConversationMessage.DIRECTION_TOOL, AiConversationMessage.CHANNEL_WEB,
                    AiDataMaskingUtil.maskPhonesAndIds(toolNode.toString()));

            return AiHelperResponse.builder()
                    .sessionId(session.getId())
                    .toolResult(toolNode)
                    .riskDisclaimer(role == AiAgentRole.TRADING_ASSISTANT ? AiRiskCopy.TRADING_DISCLAIMER : null)
                    .pendingConfirmationToken(toStringOrNull(result.get("confirmationToken")))
                    .notes(List.of("Structured tool invoked; financial reads are audited."))
                    .build();
        }

        String safe = AiPromptInjectionGuard.sanitizeUserMessage(request.getMessage());
        if (!StringUtils.hasText(safe)) {
            throw new IllegalArgumentException("message is required when structuredToolCall is absent");
        }
        auditService.appendMessage(session, AiConversationMessage.DIRECTION_USER, AiConversationMessage.CHANNEL_WEB,
                AiDataMaskingUtil.maskPhonesAndIds(safe));

        AlmukhtarAiService ai = aiServiceProvider.getIfAvailable();
        String assistant;
        int sources = 0;
        if (ai != null) {
            var chat = ai.chatForAiHelper(safe, user.getId(), tenantId, role, "aihelper_" + session.getId());
            assistant = chat.getMessage();
            sources = chat.getSourcesCount();
        } else {
            assistant = "AI text generation is disabled. Send structuredToolCall for audited read-only data, "
                    + "or enable almukhtar.ai.enabled for RAG-backed answers.";
        }
        auditService.appendMessage(session, AiConversationMessage.DIRECTION_ASSISTANT, AiConversationMessage.CHANNEL_WEB,
                AiDataMaskingUtil.maskPhonesAndIds(assistant));

        return AiHelperResponse.builder()
                .sessionId(session.getId())
                .assistantReplyMasked(assistant)
                .notes(sources > 0 ? List.of("RAG sources used: " + sources) : Collections.emptyList())
                .riskDisclaimer(role == AiAgentRole.TRADING_ASSISTANT ? AiRiskCopy.TRADING_DISCLAIMER : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AiConversationSession> listSessions(User user) {
        return sessionRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public AiConversationSession getSession(User actor, Long sessionId) {
        if (actor.getRole() == UserRole.PLATFORM_OWNER || actor.getRole() == UserRole.SUPER_ADMIN) {
            return sessionRepository.findById(sessionId).orElseThrow(() -> new AccessDeniedException("session not found"));
        }
        return sessionRepository.findByIdAndUser_Id(sessionId, actor.getId())
                .orElseThrow(() -> new AccessDeniedException("session not found"));
    }

    @Transactional
    public AiHelperResponse confirmTool(User actor, AiToolConfirmationRequest req) {
        AiConversationSession session = sessionRepository.findByIdAndUser_Id(req.getSessionId(), actor.getId())
                .orElseThrow(() -> new AccessDeniedException("session not found"));
        if (!Boolean.TRUE.equals(req.getConfirm())) {
            session.setPendingActionJson(null);
            sessionRepository.save(session);
            return AiHelperResponse.builder()
                    .sessionId(session.getId())
                    .assistantReplyMasked("Pending action cleared.")
                    .build();
        }
        if (session.getPendingActionJson() == null || session.getPendingActionJson().isBlank()) {
            throw new IllegalStateException("No pending confirmation for this session");
        }
        try {
            JsonNode pending = objectMapper.readTree(session.getPendingActionJson());
            String token = pending.path("confirmationToken").asText(null);
            if (token == null || !token.equals(req.getConfirmationToken())) {
                throw new AccessDeniedException("Invalid confirmation token");
            }
            session.setPendingActionJson(null);
            sessionRepository.save(session);
            return AiHelperResponse.builder()
                    .sessionId(session.getId())
                    .assistantReplyMasked("Confirmation accepted. Submit the payload through the normal authenticated API.")
                    .toolResult(pending)
                    .build();
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read pending action", e);
        }
    }

    private static String toStringOrNull(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    static AiAgentRole defaultRole(UserRole role) {
        return switch (role) {
            case INDIVIDUAL_USER, CORPORATE_ADMIN -> AiAgentRole.CUSTOMER_HELPER;
            case CASHIER -> AiAgentRole.CASHIER_COPILOT;
            case BRANCH_MANAGER, MOTHER_BRANCH_ADMIN, AUDITOR -> AiAgentRole.BRANCH_MANAGER_COPILOT;
            case PLATFORM_OWNER, SUPER_ADMIN -> AiAgentRole.PLATFORM_OWNER_COPILOT;
        };
    }

    private AiAgentRole resolveEffectiveRole(User user, AiAgentRole requested) {
        AiAgentRole effective = requested != null ? requested : defaultRole(user.getRole());
        if (!compatible(user.getRole(), effective)) {
            throw new AccessDeniedException("AI agent role not permitted for this account");
        }
        return effective;
    }

    private static boolean compatible(UserRole ur, AiAgentRole ar) {
        if (ur == UserRole.PLATFORM_OWNER || ur == UserRole.SUPER_ADMIN) {
            return true;
        }
        return switch (ar) {
            case PLATFORM_OWNER_COPILOT -> false;
            case BRANCH_MANAGER_COPILOT -> ur == UserRole.BRANCH_MANAGER || ur == UserRole.MOTHER_BRANCH_ADMIN
                    || ur == UserRole.AUDITOR;
            case CASHIER_COPILOT -> ur == UserRole.CASHIER;
            case CUSTOMER_HELPER -> ur == UserRole.INDIVIDUAL_USER || ur == UserRole.CORPORATE_ADMIN;
            case TRADING_ASSISTANT -> ur == UserRole.INDIVIDUAL_USER || ur == UserRole.CORPORATE_ADMIN
                    || ur == UserRole.BRANCH_MANAGER || ur == UserRole.MOTHER_BRANCH_ADMIN;
        };
    }
}
