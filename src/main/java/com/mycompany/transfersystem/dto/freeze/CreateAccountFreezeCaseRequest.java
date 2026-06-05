package com.mycompany.transfersystem.dto.freeze;

import com.mycompany.transfersystem.entity.AccountFreezeCase;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateAccountFreezeCaseRequest {
    @NotNull
    private Long userId;
    private Long walletId;
    @NotNull
    private AccountFreezeCase.CustomerVisibleCategory customerCategory;
    @NotNull
    private String internalReason;
    @NotNull
    private String customerMessage;
    private LocalDateTime slaDueAt;
    private String appealOrDisputeUrl;
}
