package com.mycompany.transfersystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseRequest {
    
    @NotBlank(message = "Release passcode is required")
    private String passcode;
    
    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    // Manual getters and setters for Lombok compatibility
    public String getPasscode() { return passcode; }
    public void setPasscode(String passcode) { this.passcode = passcode; }
    
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
}
