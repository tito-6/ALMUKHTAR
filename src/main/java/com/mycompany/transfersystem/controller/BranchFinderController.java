package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.geo.NearbyBranchRequest;
import com.mycompany.transfersystem.dto.geo.NearbyBranchResponse;
import com.mycompany.transfersystem.service.geo.BranchGeoService;
import com.mycompany.transfersystem.service.geo.PrivacyLocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nearest-branch finder. User coordinates are never persisted or audited.
 */
@RestController
@RequestMapping("/api/branches")
@CrossOrigin(origins = "*")
public class BranchFinderController {

    private final BranchGeoService branchGeoService;
    private final PrivacyLocationService privacyLocationService;

    public BranchFinderController(BranchGeoService branchGeoService,
                                  PrivacyLocationService privacyLocationService) {
        this.branchGeoService = branchGeoService;
        this.privacyLocationService = privacyLocationService;
    }

    @PostMapping("/nearby")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NearbyBranchResponse>> findNearby(@Valid @RequestBody NearbyBranchRequest request) {
        privacyLocationService.validate(request);
        try {
            List<NearbyBranchResponse> results = branchGeoService.findNearest(request);
            return ResponseEntity.ok(results);
        } finally {
            privacyLocationService.clearAfterUse(request);
        }
    }
}
