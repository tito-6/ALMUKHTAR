package com.mycompany.transfersystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for creating / updating a {@code Branch} from the admin UI.
 * Time fields are accepted as "HH:mm" or "HH:mm:ss" strings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequest {

    @NotBlank(message = "Branch name is required")
    private String name;

    private String city;
    private String country;
    private String addressLine;
    private String phone;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String opensAt;
    private String closesAt;

    /** JSON array of services, e.g. ["TOPUP","TRANSFER","QR_RELEASE","EXCHANGE"]. */
    private String services;
}
