package com.mycompany.transfersystem.service.geo;

import com.mycompany.transfersystem.dto.geo.NearbyBranchRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Validates location request and ensures coordinates are never stored or logged.
 * Call clearAfterUse(request) after the controller returns to null out coordinates.
 */
@Service
public class PrivacyLocationService {

    private static final BigDecimal MAX_RADIUS_KM = BigDecimal.valueOf(50);

    public void validate(NearbyBranchRequest request) {
        if (request.getUserLat() == null || request.getUserLng() == null) {
            throw new IllegalArgumentException("userLat and userLng are required");
        }
        if (request.getRadiusKm() != null && request.getRadiusKm().compareTo(MAX_RADIUS_KM) > 0) {
            request.setRadiusKm(MAX_RADIUS_KM);
        }
    }

    /** Call after processing to help GC and privacy (belt-and-suspenders). */
    public void clearAfterUse(NearbyBranchRequest request) {
        if (request != null) {
            request.setUserLat(null);
            request.setUserLng(null);
        }
    }
}
