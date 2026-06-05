package com.mycompany.transfersystem.entity;

import com.mycompany.transfersystem.entity.enums.NotificationChannel;
import com.mycompany.transfersystem.entity.enums.NotificationMessageCategory;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_templates_key_locale_channel",
                columnNames = {"template_key", "locale", "channel"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key", nullable = false, length = 120)
    private String templateKey;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(length = 255)
    private String subject;

    @Column(name = "body_template", columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_category", nullable = false, length = 20)
    @Builder.Default
    private NotificationMessageCategory messageCategory = NotificationMessageCategory.OPERATIONAL;

    /**
     * Approved Meta Cloud API template name; when null the provider sends rendered {@link #bodyTemplate} as plain text.
     */
    @Column(name = "meta_template_name", length = 255)
    private String metaTemplateName;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
