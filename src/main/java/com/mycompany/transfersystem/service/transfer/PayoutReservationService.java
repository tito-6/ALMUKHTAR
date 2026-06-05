package com.mycompany.transfersystem.service.transfer;

import com.mycompany.transfersystem.dto.payout.CreatePayoutReservationRequest;
import com.mycompany.transfersystem.entity.*;
import com.mycompany.transfersystem.exception.ConditionNotMetException;
import com.mycompany.transfersystem.exception.InsufficientFundsException;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayoutReservationService {

    private final PayoutReservationRepository payoutReservationRepository;
    private final BranchRepository branchRepository;
    private final BranchCashInventoryRepository branchCashInventoryRepository;
    private final CashierShiftRepository cashierShiftRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public PayoutReservation create(CreatePayoutReservationRequest req, User receiver) {
        if (!req.getPickupWindowEnd().isAfter(req.getPickupWindowStart())) {
            throw new ConditionNotMetException("Pickup window end must be after start");
        }
        Branch branch = branchRepository.findById(req.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + req.getBranchId()));
        String currency = normalizeCurrency(req.getCurrency());
        BigDecimal amount = money(req.getAmount());

        BigDecimal overlap = payoutReservationRepository.sumActiveOverlapping(
                branch.getId(), currency, PayoutReservation.ReservationStatus.ACTIVE,
                req.getPickupWindowStart(), req.getPickupWindowEnd());

        BranchCashInventory inv = branchCashInventoryRepository.findByBranchIdAndCurrency(branch.getId(), currency)
                .orElseThrow(() -> new InsufficientFundsException("No cash inventory for branch in " + currency));

        BigDecimal freeForForecast = money(inv.getAvailableBalance().subtract(inv.getReservedBalance()).subtract(overlap));
        if (freeForForecast.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Branch cannot cover this pickup reservation in "
                    + currency + " for the selected window (forecast includes existing reservations).");
        }

        LocalDateTime expires = req.getExpiresAt() != null ? req.getExpiresAt() : req.getPickupWindowEnd();
        if (!expires.isAfter(LocalDateTime.now())) {
            throw new ConditionNotMetException("Reservation must expire in the future");
        }

        PayoutReservation saved = payoutReservationRepository.save(PayoutReservation.builder()
                .receiver(receiver)
                .branch(branch)
                .currency(currency)
                .amount(amount)
                .pickupWindowStart(req.getPickupWindowStart())
                .pickupWindowEnd(req.getPickupWindowEnd())
                .expiresAt(expires)
                .status(PayoutReservation.ReservationStatus.ACTIVE)
                .build());

        auditService.log("PAYOUT_RESERVATION_CREATED", "PAYOUT_RESERVATION", saved.getId(),
                branch.getId() + " " + currency + " " + amount, receiver);

        notificationService.notifyUserPhones(receiver, "Pickup reserved",
                "Branch " + branch.getName() + ", " + currency + " " + amount + ", ref " + saved.getId());
        notificationService.notifyBranchOperation(branch.getId(), "Pickup reservation",
                "Ref " + saved.getId() + " " + currency + " " + amount + " window " + req.getPickupWindowStart());

        for (CashierShift shift : cashierShiftRepository.findByBranchIdAndStatus(branch.getId(), CashierShift.ShiftStatus.OPEN)) {
            notificationService.notifyUserPhones(shift.getCashier(), "Pickup reservation",
                    "Branch " + branch.getName() + " ref " + saved.getId());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<PayoutReservation> listForReceiver(User receiver) {
        return payoutReservationRepository.findByReceiver_IdAndStatus(receiver.getId(), PayoutReservation.ReservationStatus.ACTIVE);
    }

    @Transactional
    public void cancel(Long id, User receiver) {
        PayoutReservation r = payoutReservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        if (!r.getReceiver().getId().equals(receiver.getId())) {
            throw new ConditionNotMetException("Not your reservation");
        }
        if (r.getStatus() != PayoutReservation.ReservationStatus.ACTIVE) {
            return;
        }
        r.setStatus(PayoutReservation.ReservationStatus.CANCELLED);
        payoutReservationRepository.save(r);
        auditService.log("PAYOUT_RESERVATION_CANCELLED", "PAYOUT_RESERVATION", id, null, receiver);
    }

    @Transactional
    public PayoutReservation fulfillWithTransaction(Long id, Transaction tx) {
        PayoutReservation r = payoutReservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        if (r.getStatus() != PayoutReservation.ReservationStatus.ACTIVE) {
            throw new ConditionNotMetException("Reservation is not active");
        }
        r.setStatus(PayoutReservation.ReservationStatus.FULFILLED);
        r.setTransaction(tx);
        return payoutReservationRepository.save(r);
    }

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void expireStale() {
        List<PayoutReservation> stale = payoutReservationRepository.findByStatusAndExpiresAtBefore(
                PayoutReservation.ReservationStatus.ACTIVE, LocalDateTime.now());
        for (PayoutReservation r : stale) {
            r.setStatus(PayoutReservation.ReservationStatus.EXPIRED);
            payoutReservationRepository.save(r);
            auditService.log("PAYOUT_RESERVATION_EXPIRED", "PAYOUT_RESERVATION", r.getId(), null, null);
            notificationService.notifyUserPhones(r.getReceiver(), "Pickup reservation expired",
                    "Ref " + r.getId() + " at branch " + r.getBranch().getName());
        }
        if (!stale.isEmpty()) {
            log.info("Expired {} payout reservations", stale.size());
        }
    }

    private static String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase();
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(4) : v.setScale(4, RoundingMode.HALF_UP);
    }
}
