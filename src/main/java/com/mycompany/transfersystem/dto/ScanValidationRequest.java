package com.mycompany.transfersystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanValidationRequest {
    @NotBlank(message = "Encrypted payload is required")
    private String encryptedPayload;

    @NotBlank(message = "Token hash is required")
    private String tokenHash;

    @NotBlank(message = "TOTP code is required")
    @Size(min = 6, max = 6, message = "TOTP code must be 6 digits")
    private String totpCode;
}
