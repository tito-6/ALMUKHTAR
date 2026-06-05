package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ai_conversation_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversationMessage {

    public static final String DIRECTION_USER = "USER";
    public static final String DIRECTION_ASSISTANT = "ASSISTANT";
    public static final String DIRECTION_SYSTEM = "SYSTEM";
    public static final String DIRECTION_TOOL = "TOOL";

    public static final String CHANNEL_WEB = "WEB";
    public static final String CHANNEL_VOICE = "VOICE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AiConversationSession session;

    @Column(nullable = false, length = 16)
    private String direction;

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String channel = CHANNEL_WEB;

    @Column(name = "content_masked", nullable = false, columnDefinition = "TEXT")
    private String contentMasked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
