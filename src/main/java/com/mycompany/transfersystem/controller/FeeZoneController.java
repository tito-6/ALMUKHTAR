package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.FeeZone;
import com.mycompany.transfersystem.service.feezone.FeeZoneService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fee-zones")
@CrossOrigin(origins = "*")
public class FeeZoneController {

    private final FeeZoneService feeZoneService;

    public FeeZoneController(FeeZoneService feeZoneService) {
        this.feeZoneService = feeZoneService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<List<FeeZone>> listZones() {
        return ResponseEntity.ok(feeZoneService.findAll());
    }
}
