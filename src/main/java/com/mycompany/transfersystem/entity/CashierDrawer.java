package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cashier_drawers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashierDrawer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false, unique = true)
    private CashierShift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DrawerStatus status;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(length = 500)
    private String notes;

    public enum DrawerStatus {
        OPEN,
        CLOSED
    }
}
