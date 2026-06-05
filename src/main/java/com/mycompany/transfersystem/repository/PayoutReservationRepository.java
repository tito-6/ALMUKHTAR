package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.PayoutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PayoutReservationRepository extends JpaRepository<PayoutReservation, Long> {

    List<PayoutReservation> findByReceiver_IdAndStatus(Long receiverId, PayoutReservation.ReservationStatus status);

    @Query("""
            select coalesce(sum(r.amount), 0)
            from PayoutReservation r
            where r.branch.id = :branchId
              and r.currency = :currency
              and r.status = :status
              and r.pickupWindowStart < :windowEnd
              and r.pickupWindowEnd > :windowStart
            """)
    BigDecimal sumActiveOverlapping(@Param("branchId") Long branchId,
                                    @Param("currency") String currency,
                                    @Param("status") PayoutReservation.ReservationStatus status,
                                    @Param("windowStart") LocalDateTime windowStart,
                                    @Param("windowEnd") LocalDateTime windowEnd);

    List<PayoutReservation> findByStatusAndExpiresAtBefore(PayoutReservation.ReservationStatus status,
                                                           LocalDateTime now);

    @Query("""
            select coalesce(sum(r.amount), 0) from PayoutReservation r
            where r.branch.id = :branchId and r.currency = :currency and r.status = :status
            """)
    BigDecimal sumActiveForBranchCurrency(@Param("branchId") Long branchId,
                                                   @Param("currency") String currency,
                                                   @Param("status") PayoutReservation.ReservationStatus status);
}
