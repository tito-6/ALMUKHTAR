package com.mycompany.transfersystem.dto.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantRegistrationRequest {

    @NotBlank
    private String businessName;

    private String category;
    private String registrationNumber;
    private String address;
    private String city;
    private String country;
    private Long branchId;
}
