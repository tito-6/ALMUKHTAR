package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.AiToolName;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ai_tool_call_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiToolCallAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AiConversationSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_name", nullable = false, length = 64)
    private AiToolName toolName;

    @Column(name = "arguments_masked", columnDefinition = "TEXT")
    private String argumentsMasked;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(nullable = false)
    @Builder.Default
    private boolean ok = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
