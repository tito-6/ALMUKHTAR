package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.AiAgentRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ai_conversation_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_role", nullable = false, length = 40)
    private AiAgentRole agentRole;

    @Column(name = "external_conversation_id", length = 128)
    private String externalConversationId;

    @Column(name = "client_session_key", length = 64)
    private String clientSessionKey;

    @Column(name = "pending_action_json", columnDefinition = "TEXT")
    private String pendingActionJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
