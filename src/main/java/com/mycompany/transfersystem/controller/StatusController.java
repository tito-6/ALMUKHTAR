package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.status.BranchPickupStatusDto;
import com.mycompany.transfersystem.service.status.OperationalStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/status")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StatusController {

    private final OperationalStatusService operationalStatusService;

    @GetMapping("/public/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok(operationalStatusService.publicSummary());
    }

    @GetMapping("/branches/{branchId}/pickup")
    public ResponseEntity<BranchPickupStatusDto> pickup(@PathVariable Long branchId) {
        return ResponseEntity.ok(operationalStatusService.branchPickup(branchId));
    }
}
