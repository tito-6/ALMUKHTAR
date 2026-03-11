package com.mycompany.transfersystem.service.geo;

import com.mycompany.transfersystem.dto.geo.NearbyBranchRequest;
import com.mycompany.transfersystem.dto.geo.NearbyBranchResponse;
import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Find nearest branches by user coordinates. Does not persist user location.
 */
@Service
public class BranchGeoService {

    private static final BigDecimal MAX_RADIUS_KM = BigDecimal.valueOf(50);
    private static final int EARTH_RADIUS_KM = 6371;

    private final BranchRepository branchRepository;

    public BranchGeoService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public List<NearbyBranchResponse> findNearest(NearbyBranchRequest request) {
        BigDecimal radiusKm = request.getRadiusKm() != null ? request.getRadiusKm() : BigDecimal.TEN;
        if (radiusKm.compareTo(MAX_RADIUS_KM) > 0) {
            radiusKm = MAX_RADIUS_KM;
        }
        double userLat = request.getUserLat().doubleValue();
        double userLng = request.getUserLng().doubleValue();

        List<Branch> allWithLocation = branchRepository.findAll().stream()
                .filter(b -> b.getLatitude() != null && b.getLongitude() != null)
                .collect(Collectors.toList());

        List<NearbyBranchResponse> results = allWithLocation.stream()
                .map(b -> toResponse(b, userLat, userLng))
                .filter(r -> r.getDistanceKm().compareTo(radiusKm) <= 0)
                .sorted((a, b) -> a.getDistanceKm().compareTo(b.getDistanceKm()))
                .limit(10)
                .collect(Collectors.toList());

        return results;
    }

    private NearbyBranchResponse toResponse(Branch b, double userLat, double userLng) {
        double dist = haversineKm(userLat, userLng,
                b.getLatitude().doubleValue(), b.getLongitude().doubleValue());
        NearbyBranchResponse r = new NearbyBranchResponse();
        r.setBranchId(b.getId());
        r.setName(b.getName());
        r.setAddress(b.getAddressLine());
        r.setCity(b.getCity());
        r.setCountry(b.getCountry());
        r.setPhone(b.getPhone());
        r.setDistanceKm(BigDecimal.valueOf(dist).setScale(1, RoundingMode.HALF_UP));
        r.setIsOpenNow(computeIsOpenNow(b.getOpensAt(), b.getClosesAt()));
        r.setServices(parseServices(b.getServices()));
        return r;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private static Boolean computeIsOpenNow(LocalTime opensAt, LocalTime closesAt) {
        if (opensAt == null || closesAt == null) return null;
        LocalTime now = LocalTime.now();
        if (opensAt.isBefore(closesAt)) {
            return !now.isBefore(opensAt) && now.isBefore(closesAt);
        }
        return !now.isBefore(opensAt) || now.isBefore(closesAt);
    }

    private static List<String> parseServices(String services) {
        if (services == null || services.isBlank()) return List.of();
        return List.of(services.replace("[", "").replace("]", "").replace("\"", "").split("\\s*,\\s*"));
    }
}
