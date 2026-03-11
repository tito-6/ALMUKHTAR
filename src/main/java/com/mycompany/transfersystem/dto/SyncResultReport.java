package com.mycompany.transfersystem.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class SyncResultReport {
    private int applied;
    private int conflicts;
    private int rejected;
    private List<String> errors;
    private Instant processedAt;
}
