package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "branch_incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Builder.Default
    @Column(name = "cash_shortage_public", nullable = false)
    private boolean cashShortagePublic = false;

    @Builder.Default
    @Column(name = "integration_failure", nullable = false)
    private boolean integrationFailure = false;

    @Builder.Default
    @Column(name = "whatsapp_degraded", nullable = false)
    private boolean whatsappDegraded = false;

    @Builder.Default
    @Column(name = "branch_outage", nullable = false)
    private boolean branchOutage = false;

    @Column(name = "public_message", length = 2000)
    private String publicMessage;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
