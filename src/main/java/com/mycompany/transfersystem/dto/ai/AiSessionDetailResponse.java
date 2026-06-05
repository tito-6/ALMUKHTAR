package com.mycompany.transfersystem.dto.ai;

import com.mycompany.transfersystem.entity.enums.AiAgentRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSessionDetailResponse {

    private Long id;
    private AiAgentRole agentRole;
    private String externalConversationId;
    private Instant createdAt;
    private List<MessageLine> messages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageLine {
        private String direction;
        private String channel;
        private String contentMasked;
        private Instant createdAt;
    }
}
