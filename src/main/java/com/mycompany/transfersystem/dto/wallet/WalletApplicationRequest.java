package com.mycompany.transfersystem.dto.wallet;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class WalletApplicationRequest {

    private String fullName;
    private String dateOfBirth;
    private String address;
    /** Document references (file_reference from upload) by doc type */
    @NotNull
    private List<DocumentReference> documents;

    @Data
    public static class DocumentReference {
        private String docType; // NATIONAL_ID, PASSPORT, SELFIE, PROOF_OF_ADDRESS
        private String fileReference;
    }
}
