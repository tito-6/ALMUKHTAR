package com.mycompany.transfersystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String errorCode;
    private String path;
    private String requestId;
    private List<FieldErrorDetail> fieldErrors;

    /** Structured machine-readable context (e.g. trading failure codes). */
    private Map<String, Object> details;

    public record FieldErrorDetail(String field, Object rejectedValue, String message) {}
}
