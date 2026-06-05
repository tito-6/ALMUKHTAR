package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavingsGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal targetAmount;

    @Column(name = "saved_amount", nullable = false, precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal savedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SavingsGoalStatus status = SavingsGoalStatus.ACTIVE;

    @Column(name = "auto_sweep")
    @Builder.Default
    private boolean autoSweep = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    public enum SavingsGoalStatus { ACTIVE, ACHIEVED, CANCELLED }
}
