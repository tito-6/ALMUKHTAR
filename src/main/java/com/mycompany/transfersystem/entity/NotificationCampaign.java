package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_campaigns")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationCampaign {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false, length = 30)
    private TargetAudience targetAudience;
    @Column(name = "message_template", columnDefinition = "TEXT")
    private String messageTemplate;
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    @Column(name = "sent_count")
    @Builder.Default
    private int sentCount = 0;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;
    @Column(name = "created_by")
    private Long createdBy;
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    public enum TargetAudience { ALL, HIGH_VALUE, DORMANT, NEW_USERS }
    public enum CampaignStatus { DRAFT, SCHEDULED, SENT, CANCELLED }
}
