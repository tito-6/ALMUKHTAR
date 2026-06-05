package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Dispute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporterUser;
    @Column(name = "transaction_id")
    private Long transactionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisputeCategory category;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;
    @Column(name = "assigned_branch_id")
    private Long assignedBranchId;
    @Column(name = "sla_met")
    private Boolean slaMet;
    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;
    @Column(columnDefinition = "TEXT")
    private String resolution;
    @Column(name = "resolved_by")
    private Long resolvedBy;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum DisputeCategory { WRONG_AMOUNT, UNAUTHORIZED, DOUBLE_CHARGE, OTHER }
    public enum DisputeStatus { OPEN, UNDER_REVIEW, RESOLVED, REJECTED }
}
