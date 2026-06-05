package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.VaultDocument;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.vault.DocumentVaultService;
import com.mycompany.transfersystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vault")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DocumentVaultController {

    private final DocumentVaultService documentVaultService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VaultDocument> upload(@RequestParam MultipartFile file,
                                                  @RequestParam String documentType,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(documentVaultService.uploadDocument(user.getId(), file, documentType));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VaultDocument>> list(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(documentVaultService.listDocuments(user.getId()));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> download(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        String url = documentVaultService.downloadDocument(id, user.getId());
        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        documentVaultService.deleteDocument(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
