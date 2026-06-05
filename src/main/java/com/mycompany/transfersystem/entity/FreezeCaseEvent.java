package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "freeze_case_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreezeCaseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freeze_case_id", nullable = false)
    private AccountFreezeCase freezeCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private EventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "internal_note", length = 4000)
    private String internalNote;

    @Column(name = "customer_safe_summary", length = 2000)
    private String customerSafeSummary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum EventType {
        CREATED,
        STATUS_CHANGED,
        APPEAL_SUBMITTED,
        INTERNAL_NOTE,
        SLA_EXTENDED,
        RELEASED
    }
}
