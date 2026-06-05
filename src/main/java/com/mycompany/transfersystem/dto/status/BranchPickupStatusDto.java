package com.mycompany.transfersystem.dto.status;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BranchPickupStatusDto {
    Long branchId;
    String branchName;
    String city;
    boolean pickupPossible;
    String publicMessage;
    boolean cashShortageFlag;
    boolean branchOutage;
    boolean integrationDegraded;
    boolean whatsappDegradedAtBranch;
}
