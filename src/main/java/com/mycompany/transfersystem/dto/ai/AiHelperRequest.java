package com.mycompany.transfersystem.dto.ai;

import com.mycompany.transfersystem.entity.enums.AiAgentRole;
import com.mycompany.transfersystem.entity.enums.AiToolName;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiHelperRequest {

    /**
     * End-user text; masked before persistence. Optional when {@link #structuredToolCall} is set.
     */
    private String message;

    /**
     * When null, derived from the authenticated user's {@code UserRole}.
     */
    private AiAgentRole agentRole;

    /**
     * Optional stable key to reuse the same {@link com.mycompany.transfersystem.entity.AiConversationSession}.
     */
    private String sessionKey;

    private StructuredToolCall structuredToolCall;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StructuredToolCall {
        @NotNull
        private AiToolName tool;
        private Map<String, Object> arguments;
    }
}
