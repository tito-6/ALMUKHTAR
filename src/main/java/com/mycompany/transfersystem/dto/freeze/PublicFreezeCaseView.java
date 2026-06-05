package com.mycompany.transfersystem.dto.freeze;

import com.mycompany.transfersystem.entity.AccountFreezeCase;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PublicFreezeCaseView {
    String publicCaseId;
    AccountFreezeCase.CustomerVisibleCategory customerCategory;
    String customerMessage;
    LocalDateTime slaDueAt;
    String appealOrDisputeUrl;
    AccountFreezeCase.CaseStatus status;
}
