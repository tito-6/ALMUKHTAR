package com.mycompany.transfersystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyncQueueResponse {
    private Long queueId;
    private String status;
}
