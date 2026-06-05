package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "split_participants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SplitParticipant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_request_id", nullable = false)
    private SplitRequest splitRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_user_id", nullable = false)
    private User payeeUser;

    @Column(name = "share_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal shareAmount;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Builder.Default
    private boolean paid = false;
}
