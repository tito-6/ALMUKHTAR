package com.mycompany.transfersystem.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.transfersystem.entity.AiConversationMessage;
import com.mycompany.transfersystem.entity.AiConversationSession;
import com.mycompany.transfersystem.entity.AiToolCallAudit;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.enums.AiAgentRole;
import com.mycompany.transfersystem.entity.enums.AiToolName;
import com.mycompany.transfersystem.repository.AiConversationMessageRepository;
import com.mycompany.transfersystem.repository.AiConversationSessionRepository;
import com.mycompany.transfersystem.repository.AiToolCallAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiConversationAuditService {

    private final AiConversationSessionRepository sessionRepository;
    private final AiConversationMessageRepository messageRepository;
    private final AiToolCallAuditRepository toolCallAuditRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiConversationSession findOrCreateSession(User user, Long tenantId, AiAgentRole agentRole, String sessionKey) {
        if (sessionKey != null && !sessionKey.isBlank()) {
            return sessionRepository.findByUser_IdAndClientSessionKey(user.getId(), sessionKey)
                    .orElseGet(() -> sessionRepository.save(AiConversationSession.builder()
                            .user(user)
                            .tenantId(tenantId)
                            .agentRole(agentRole)
                            .clientSessionKey(sessionKey)
                            .build()));
        }
        AiConversationSession s = AiConversationSession.builder()
                .user(user)
                .tenantId(tenantId)
                .agentRole(agentRole)
                .build();
        return sessionRepository.save(s);
    }

    @Transactional
    public AiConversationSession touchExternalConversation(AiConversationSession session, String externalId) {
        session.setExternalConversationId(externalId);
        return sessionRepository.save(session);
    }

    @Transactional
    public void appendMessage(AiConversationSession session, String direction, String channel, String maskedContent) {
        messageRepository.save(AiConversationMessage.builder()
                .session(session)
                .direction(direction)
                .channel(channel != null ? channel : AiConversationMessage.CHANNEL_WEB)
                .contentMasked(maskedContent)
                .build());
    }

    @Transactional
    public void recordToolCall(AiConversationSession session,
                               User user,
                               Long tenantId,
                               AiToolName tool,
                               Object argumentsMasked,
                               String resultSummary,
                               boolean ok) {
        String argsJson = safeJson(argumentsMasked);
        toolCallAuditRepository.save(AiToolCallAudit.builder()
                .session(session)
                .user(user)
                .tenantId(tenantId)
                .toolName(tool)
                .argumentsMasked(argsJson)
                .resultSummary(resultSummary != null ? AiDataMaskingUtil.maskPhonesAndIds(resultSummary) : null)
                .ok(ok)
                .build());
    }

    @Transactional
    public void savePendingAction(AiConversationSession session, Object pending) {
        session.setPendingActionJson(safeJson(pending));
        sessionRepository.save(session);
    }

    private String safeJson(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialization_failed\"}";
        }
    }
}
