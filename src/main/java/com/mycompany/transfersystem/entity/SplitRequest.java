package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "split_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SplitRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_user_id", nullable = false)
    private User initiatorUser;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SplitStatus status = SplitStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum SplitStatus { PENDING, PARTIALLY_PAID, COMPLETED, EXPIRED, CANCELLED }
}
