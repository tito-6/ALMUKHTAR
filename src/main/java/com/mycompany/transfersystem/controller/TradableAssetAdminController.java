package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.trading.UpsertTradableAssetRequest;
import com.mycompany.transfersystem.entity.TradableAsset;
import com.mycompany.transfersystem.repository.TradableAssetRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trading/admin/tradable-assets")
@CrossOrigin(origins = "*")
public class TradableAssetAdminController {

    private final TradableAssetRepository tradableAssetRepository;

    public TradableAssetAdminController(TradableAssetRepository tradableAssetRepository) {
        this.tradableAssetRepository = tradableAssetRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<List<TradableAsset>> list() {
        return ResponseEntity.ok(tradableAssetRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<TradableAsset> create(@Valid @RequestBody UpsertTradableAssetRequest req) {
        TradableAsset a = TradableAsset.builder()
                .tenantId(req.getTenantId())
                .countryCode(req.getCountryCode())
                .allowedRolesCsv(req.getAllowedRolesCsv())
                .symbol(req.getSymbol().trim().toUpperCase())
                .assetClass(req.getAssetClass())
                .displayName(req.getDisplayName())
                .currency(req.getCurrency())
                .market(req.getMarket())
                .enabled(req.isEnabled())
                .minOrderValue(req.getMinOrderValue())
                .maxOrderValue(req.getMaxOrderValue())
                .riskLevel(req.getRiskLevel())
                .build();
        return ResponseEntity.ok(tradableAssetRepository.save(a));
    }
}
