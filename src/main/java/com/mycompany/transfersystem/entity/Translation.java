package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Entity
@Table(name = "translations", uniqueConstraints = @UniqueConstraint(columnNames = {"locale", "message_key"}))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Translation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 10)
    private String locale;
    @Column(name = "message_key", nullable = false, length = 200)
    private String messageKey;
    @Column(name = "message_value", columnDefinition = "TEXT", nullable = false)
    private String messageValue;
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
