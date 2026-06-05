package com.mycompany.transfersystem.dto.sync;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveSyncConflictRequest {
    @NotNull
    private Decision decision;
    private String managerNote;

    public enum Decision {
        APPLY,
        DISCARD
    }
}
