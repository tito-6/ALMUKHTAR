package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "family_groups")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FamilyGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(name = "monthly_spending_limit", precision = 20, scale = 4)
    private BigDecimal monthlySpendingLimit;
    @Column(nullable = false, length = 10)
    private String currency;
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
