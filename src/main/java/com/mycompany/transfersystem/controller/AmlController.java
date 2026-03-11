package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.AmlAlert;
import com.mycompany.transfersystem.repository.AmlAlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aml")
@CrossOrigin(origins = "*")
public class AmlController {

    private final AmlAlertRepository amlAlertRepository;

    public AmlController(AmlAlertRepository amlAlertRepository) {
        this.amlAlertRepository = amlAlertRepository;
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('AUDITOR','PLATFORM_OWNER')")
    public ResponseEntity<List<AmlAlert>> getAlerts() {
        return ResponseEntity.ok(amlAlertRepository.findAll());
    }
}
