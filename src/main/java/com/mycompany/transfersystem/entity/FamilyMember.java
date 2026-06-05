package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "family_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FamilyMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_group_id", nullable = false)
    private FamilyGroup familyGroup;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_user_id", nullable = false)
    private User memberUser;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FamilyRole role;
    @Column(name = "monthly_spending_limit", precision = 20, scale = 4)
    private BigDecimal monthlySpendingLimit;
    @Builder.Default
    private boolean active = true;
    public enum FamilyRole { OWNER, MEMBER }
}
