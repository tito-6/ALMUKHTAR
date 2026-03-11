package com.mycompany.transfersystem.dto.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NearbyBranchRequest {

    @NotNull
    @DecimalMin("-90") @DecimalMax("90")
    private BigDecimal userLat;

    @NotNull
    @DecimalMin("-180") @DecimalMax("180")
    private BigDecimal userLng;

    /** Max radius in km (clamped to 50). */
    private BigDecimal radiusKm = BigDecimal.valueOf(10);

    /** Optional filter: TOPUP, TRANSFER, QR_RELEASE, EXCHANGE */
    private List<String> servicesRequired;
}
