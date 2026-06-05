package com.mycompany.transfersystem.service.liquidity;

import com.mycompany.transfersystem.dto.cashier.CloseShiftRequest;
import com.mycompany.transfersystem.dto.cashier.OpenShiftRequest;
import com.mycompany.transfersystem.entity.CashierDrawer;
import com.mycompany.transfersystem.entity.CashierShift;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.CashierDrawerRepository;
import com.mycompany.transfersystem.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Liquidity-facing cashier shift operations with physical {@link CashierDrawer} lifecycle.
 * Delegates core shift balances to {@link com.mycompany.transfersystem.service.cashier.CashierShiftService}.
 */
@Service("hawalaLiquidityCashierShiftService")
@RequiredArgsConstructor
public class CashierShiftService {

    private final com.mycompany.transfersystem.service.cashier.CashierShiftService coreCashierShiftService;
    private final CashierDrawerRepository cashierDrawerRepository;
    private final AuditService auditService;

    @Transactional
    public CashierShift openShiftWithDrawer(OpenShiftRequest request, User cashier) {
        CashierShift shift = coreCashierShiftService.openShift(request, cashier);
        cashierDrawerRepository.save(CashierDrawer.builder()
                .shift(shift)
                .status(CashierDrawer.DrawerStatus.OPEN)
                .notes(request.getNotes())
                .build());
        auditService.log("DRAWER_OPENED_WITH_SHIFT", "CASHIER_SHIFT", shift.getId(), null, cashier);
        return shift;
    }

    @Transactional
    public CashierShift closeShiftWithDrawer(CloseShiftRequest request, User cashier) {
        CashierShift shift = coreCashierShiftService.closeShift(request, cashier);
        cashierDrawerRepository.findByShiftId(shift.getId()).ifPresent(drawer -> {
            if (drawer.getStatus() == CashierDrawer.DrawerStatus.OPEN) {
                drawer.setStatus(CashierDrawer.DrawerStatus.CLOSED);
                drawer.setClosedAt(LocalDateTime.now());
                cashierDrawerRepository.save(drawer);
            }
        });
        auditService.log("DRAWER_CLOSED_WITH_SHIFT", "CASHIER_SHIFT", shift.getId(), null, cashier);
        return shift;
    }

    @Transactional(readOnly = true)
    public CashierDrawer getDrawerForShift(Long shiftId) {
        return cashierDrawerRepository.findByShiftId(shiftId)
                .orElseThrow(() -> new com.mycompany.transfersystem.exception.ResourceNotFoundException("Drawer for shift not found: " + shiftId));
    }
}
