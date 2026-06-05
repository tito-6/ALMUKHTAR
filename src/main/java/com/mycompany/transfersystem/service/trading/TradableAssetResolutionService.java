package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.context.TenantContextHolder;
import com.mycompany.transfersystem.entity.TradableAsset;
import com.mycompany.transfersystem.entity.enums.UserRole;
import com.mycompany.transfersystem.repository.TradableAssetRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TradableAssetResolutionService {

    private final TradableAssetRepository tradableAssetRepository;

    public TradableAssetResolutionService(TradableAssetRepository tradableAssetRepository) {
        this.tradableAssetRepository = tradableAssetRepository;
    }

    /**
     * Resolves the tradable asset rule row for a symbol, preferring the most specific tenant/country match.
     */
    public Optional<TradableAsset> resolveEnabled(String symbol, UserRole role) {
        Long tenantId = TenantContextHolder.getTenantId();
        return resolveEnabled(symbol, tenantId, defaultCountry(), role);
    }

    public Optional<TradableAsset> resolveEnabled(String symbol, Long tenantId, String countryCode, UserRole role) {
        String sym = symbol == null ? "" : symbol.trim().toUpperCase();
        List<TradableAsset> rows = tradableAssetRepository.findBySymbolIgnoreCaseAndEnabledIsTrue(sym);
        return rows.stream()
                .filter(a -> matchesTenant(a, tenantId))
                .filter(a -> matchesCountry(a, countryCode))
                .filter(a -> matchesRole(a, role))
                .max(Comparator.comparingInt(a -> specificityScore(a, tenantId, countryCode)));
    }

    private static String defaultCountry() {
        return "SY";
    }

    private static boolean matchesTenant(TradableAsset a, Long tenantId) {
        if (a.getTenantId() == null) {
            return true;
        }
        return tenantId != null && a.getTenantId().equals(tenantId);
    }

    private static boolean matchesCountry(TradableAsset a, String countryCode) {
        if (a.getCountryCode() == null || a.getCountryCode().isBlank()) {
            return true;
        }
        return countryCode != null && a.getCountryCode().equalsIgnoreCase(countryCode);
    }

    private static boolean matchesRole(TradableAsset a, UserRole role) {
        if (a.getAllowedRolesCsv() == null || a.getAllowedRolesCsv().isBlank()) {
            return true;
        }
        List<String> allowed = Arrays.stream(a.getAllowedRolesCsv().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        return allowed.contains(role.name().toUpperCase());
    }

    private static int specificityScore(TradableAsset a, Long tenantId, String countryCode) {
        int s = 0;
        if (a.getTenantId() != null && tenantId != null && a.getTenantId().equals(tenantId)) {
            s += 4;
        }
        if (a.getCountryCode() != null && countryCode != null && a.getCountryCode().equalsIgnoreCase(countryCode)) {
            s += 2;
        }
        if (a.getAllowedRolesCsv() != null && !a.getAllowedRolesCsv().isBlank()) {
            s += 1;
        }
        return s;
    }
}
