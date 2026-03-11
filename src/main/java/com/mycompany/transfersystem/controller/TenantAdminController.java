package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.Tenant;
import com.mycompany.transfersystem.repository.TenantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/tenants")
@CrossOrigin(origins = "*")
public class TenantAdminController {

    private final TenantRepository tenantRepository;

    public TenantAdminController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<List<Tenant>> list() {
        return ResponseEntity.ok(tenantRepository.findAll());
    }
}
