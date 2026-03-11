package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.wallet.KycReviewRequest;
import com.mycompany.transfersystem.dto.wallet.WalletApplicationRequest;
import com.mycompany.transfersystem.entity.KycDocument;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.WalletApplication;
import com.mycompany.transfersystem.entity.enums.KycDocumentType;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.storage.DocumentStorageService;
import com.mycompany.transfersystem.service.wallet.DocumentVerificationService;
import com.mycompany.transfersystem.service.wallet.WalletApplicationService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletApplicationController {

    private final WalletApplicationService walletApplicationService;
    private final UserRepository userRepository;
    private final DocumentStorageService documentStorageService;
    private final com.mycompany.transfersystem.repository.KycDocumentRepository kycDocumentRepository;
    private final DocumentVerificationService documentVerificationService;

    public WalletApplicationController(WalletApplicationService walletApplicationService,
                                       UserRepository userRepository,
                                       DocumentStorageService documentStorageService,
                                       com.mycompany.transfersystem.repository.KycDocumentRepository kycDocumentRepository,
                                       DocumentVerificationService documentVerificationService) {
        this.walletApplicationService = walletApplicationService;
        this.userRepository = userRepository;
        this.documentStorageService = documentStorageService;
        this.kycDocumentRepository = kycDocumentRepository;
        this.documentVerificationService = documentVerificationService;
    }

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WalletApplication> submitApplication(
            @Valid @RequestBody WalletApplicationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        WalletApplication app = walletApplicationService.submitApplication(user.getId(), request, user);
        return ResponseEntity.ok(app);
    }

    @GetMapping("/my-application")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WalletApplication> getMyApplication(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(walletApplicationService.getMyApplication(user.getId()));
    }

    @GetMapping("/pending-reviews")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<List<WalletApplication>> getPendingReviews() {
        return ResponseEntity.ok(walletApplicationService.getPendingReviews());
    }

    @PutMapping("/review/{appId}")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','MOTHER_BRANCH_ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<WalletApplication> reviewApplication(
            @PathVariable Long appId,
            @Valid @RequestBody KycReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User reviewer = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(walletApplicationService.reviewApplication(appId, request, reviewer));
    }

    @PostMapping(value = "/apply/upload-doc", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KycDocument> uploadDoc(
            @RequestParam("file") MultipartFile file,
            @RequestParam("docType") String docTypeStr,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        WalletApplication app = walletApplicationService.getMyApplication(user.getId());
        KycDocumentType docType = KycDocumentType.valueOf(docTypeStr.toUpperCase());
        String fileRef = documentStorageService.store(
                app.getId().toString(), docType.name(),
                file.getOriginalFilename(), file.getBytes());
        KycDocument doc = KycDocument.builder()
                .application(app)
                .docType(docType)
                .fileReference(fileRef)
                .uploadAt(Instant.now())
                .build();
        kycDocumentRepository.save(doc);
        documentVerificationService.analyzeDocument(doc);
        return ResponseEntity.ok(doc);
    }
}
