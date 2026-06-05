package com.mycompany.transfersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "loan_repayment_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "instalment_number", nullable = false)
    private int instalmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_due", nullable = false, precision = 20, scale = 4)
    private BigDecimal principalDue;

    @Column(name = "interest_due", nullable = false, precision = 20, scale = 4)
    private BigDecimal interestDue;

    @Column(name = "total_due", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalDue;

    @Column(name = "paid_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
