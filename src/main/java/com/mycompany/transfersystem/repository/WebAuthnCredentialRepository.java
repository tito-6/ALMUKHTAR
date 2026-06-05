package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    Optional<WebAuthnCredential> findByCredentialId(String credentialId);
    List<WebAuthnCredential> findByUserId(Long userId);
}
